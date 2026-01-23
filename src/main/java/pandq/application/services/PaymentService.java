package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.PaymentDTO;
import pandq.adapter.web.api.dtos.SepayDTO;
import pandq.adapter.web.api.dtos.ZaloPayDTO;
import pandq.application.port.repositories.OrderRepository;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.enums.PaymentMethod;
import pandq.domain.models.order.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payment Service
 * Orchestrates payment operations across different payment providers
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ZaloPayService zaloPayService;
    private final SepayService sepayService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ShippingCalculatorService shippingCalculatorService;

    // Store payment transactions (in production, use database)
    private final Map<String, PaymentTransaction> paymentTransactions = new HashMap<>();

    /**
     * Get available payment methods with detailed information
     */
    @Transactional(readOnly = true)
    public PaymentDTO.GetPaymentMethodsResponse getPaymentMethods() {
        // TODO: Implement payment methods listing
        return new PaymentDTO.GetPaymentMethodsResponse();
    }

    /**
     * Initiate payment based on payment method selected
     * Loads real data from Order with all relationships
     * Must be @Transactional to ensure lazy-loaded data is accessible
     */
    @Transactional
    public PaymentDTO.InitiatePaymentResponse initiatePayment(PaymentDTO.InitiatePaymentRequest request) {
        // TODO: Implement payment initiation
        log.info("Initiating payment for order: {}, method: {}", 
            request.getOrderId(), request.getPaymentMethod());
        
        PaymentDTO.InitiatePaymentResponse response = new PaymentDTO.InitiatePaymentResponse();
        response.setStatus("pending");
        response.setMessage("Payment initiated successfully");
        response.setTransactionId(generateTransactionId());
        
        try {
            // Load Order from database
            java.util.UUID uuid = java.util.UUID.fromString(request.getOrderId());
            Order order = orderRepository.findById(uuid).orElse(null);
            
            if (order == null) {
                response.setStatus("failed");
                response.setMessage("Order not found");
                return response;
            }
            
            // Get actual amount from order
            Long actualAmount = order.getFinalAmount() != null ? order.getFinalAmount().longValue() : 0L;
            
            // Log payment info
            log.debug("Payment for order: {}, method: {}, amount: {}", 
                request.getOrderId(),
                request.getPaymentMethod(),
                actualAmount);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error initiating payment", e);
            response.setStatus("failed");
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Get payment details for an order including all required information for checkout
     * Must use @Transactional to ensure lazy-loaded relationships are loaded
     * Also saves shipping address to order if not already set
     */
    @Transactional
    public PaymentDTO.OrderPaymentDetailsResponse getOrderPaymentDetails(String orderId) {
        PaymentDTO.OrderPaymentDetailsResponse response = new PaymentDTO.OrderPaymentDetailsResponse();
        
        try {
            log.info("Getting payment details for order: {}", orderId);
            
            // Load full order with all relationships - inline to ensure single transaction
            Order order = null;
            try {
                java.util.UUID uuid = java.util.UUID.fromString(orderId);
                order = orderRepository.findById(uuid).orElse(null);
                
                if (order != null) {
                    // Load User data
                    if (order.getUser() != null) {
                        order.getUser().getId();
                        order.getUser().getEmail();
                        order.getUser().getFullName();
                        order.getUser().getPhone();
                    }
                    
                    // Load OrderItems and Product details
                    if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        for (var item : order.getOrderItems()) {
                            if (item.getProduct() != null) {
                                item.getProduct().getId();
                                item.getProduct().getName();
                                item.getProduct().getPrice();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Exception while loading order data", e);
                response.setMessage("Error loading order: " + e.getMessage());
                return response;
            }
            
            if (order == null) {
                log.warn("Order not found: {}", orderId);
                response.setMessage("Order not found");
                return response;
            }
            
            log.debug("Order found: {}, user: {}, items: {}", 
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : "null",
                order.getOrderItems() != null ? order.getOrderItems().size() : 0);
            
            // Basic order info
            response.setOrderId(order.getId().toString());
            response.setOrderStatus(order.getStatus() != null ? order.getStatus().toString() : "UNKNOWN");
            response.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().toString() : "NOT_SET");
            
            // User info - guaranteed to be loaded by loadOrderData
            if (order.getUser() != null) {
                response.setUserId(order.getUser().getId().toString());
                response.setUserName(order.getUser().getFullName() != null ? order.getUser().getFullName() : "Anonymous");
                response.setUserEmail(order.getUser().getEmail() != null ? order.getUser().getEmail() : "");
                response.setUserPhone(order.getUser().getPhone() != null ? order.getUser().getPhone() : "");
            } else {
                log.warn("Order {} has no user!", orderId);
                response.setUserId("");
                response.setUserName("Anonymous");
                response.setUserEmail("");
                response.setUserPhone("");
            }
            
            // Shipping address - ALWAYS sync from user's current default address
            String shippingAddress = "";
            String shippingCity = null;
            String shippingDistrict = null;
            boolean needsSaveAddress = false;
            
            if (order.getUser() != null && order.getUser().getAddresses() != null) {
                // Find default address
                for (var addr : order.getUser().getAddresses()) {
                    if (addr.getIsDefault() != null && addr.getIsDefault()) {
                        shippingAddress = (addr.getDetailAddress() != null ? addr.getDetailAddress() : "");
                        if (addr.getWard() != null && !addr.getWard().isEmpty()) {
                            shippingAddress += ", " + addr.getWard();
                        }
                        shippingCity = addr.getCity();
                        shippingDistrict = addr.getDistrict();
                        
                        // ALWAYS update order's shipping address from user's current default
                        String fullAddress = shippingAddress;
                        if (shippingDistrict != null) fullAddress += ", " + shippingDistrict;
                        if (shippingCity != null) fullAddress += ", " + shippingCity;
                        
                        // Check if address changed
                        if (order.getShippingAddress() == null || !order.getShippingAddress().equals(fullAddress)) {
                            order.setShippingAddress(fullAddress);
                            needsSaveAddress = true;
                            log.info("Updating shipping address for order {}: {}", orderId, fullAddress);
                        }
                        
                        log.debug("Using default address for order {}", orderId);
                        break;
                    }
                }
            }
            
            // Save order if shipping address was updated
            if (needsSaveAddress) {
                orderRepository.save(order);
            }
            
            // Fallback to order's shipping address if no default found
            if (shippingAddress.isEmpty() && order.getShippingAddress() != null) {
                shippingAddress = order.getShippingAddress();
                log.debug("Using order snapshot address for order {}", orderId);
            }
            
            response.setShippingAddress(shippingAddress);
            response.setShippingCity(shippingCity);
            response.setShippingDistrict(shippingDistrict);
            
            // ==================== TÍNH PHÍ VẬN CHUYỂN THEO VÙNG ====================
            // Tính phí ship dựa trên thành phố/tỉnh của khách hàng
            BigDecimal calculatedShippingFee = ShippingFeeCalculator.calculateShippingFee(shippingCity);
            
            // Kiểm tra nếu shipping fee thay đổi thì cập nhật Order
            if (order.getShippingFee() == null || order.getShippingFee().compareTo(calculatedShippingFee) != 0) {
                order.setShippingFee(calculatedShippingFee);
                
                // Recalculate finalAmount = totalAmount + shippingFee - discountAmount
                BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
                BigDecimal newFinalAmount = totalAmount.add(calculatedShippingFee).subtract(discountAmount);
                order.setFinalAmount(newFinalAmount);
                
                // Lưu Order với shipping fee mới
                orderRepository.save(order);
                log.info("Updated shipping fee for order {}: {} (city: {})", orderId, calculatedShippingFee, shippingCity);
            }
            // ====================================================================
            
            // Order items
            List<PaymentDTO.OrderItemDetail> items = new ArrayList<>();
            Long subtotal = 0L;
            
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                for (var item : order.getOrderItems()) {
                    if (item.getProduct() != null) {
                        PaymentDTO.OrderItemDetail detail = new PaymentDTO.OrderItemDetail();
                        
                        detail.setProductId(item.getProduct().getId().toString());
                        detail.setProductName(item.getProduct().getName() != null ? item.getProduct().getName() : "Unknown Product");
                        detail.setPrice(item.getPrice() != null ? item.getPrice().longValue() : 0L);
                        detail.setQuantity(item.getQuantity() != null ? item.getQuantity() : 0);
                        detail.setTotalPrice(item.getTotalPrice() != null ? item.getTotalPrice().longValue() : 0L);
                        items.add(detail);
                        
                        Long itemTotal = item.getTotalPrice() != null ? item.getTotalPrice().longValue() : 0L;
                        subtotal += itemTotal;
                        
                        log.debug("Added item: {} ({}x{} = {})", 
                            item.getProduct().getName(), 
                            item.getQuantity(), 
                            item.getPrice(), 
                            item.getTotalPrice());
                    } else {
                        log.warn("OrderItem {} has no product!", item.getId());
                    }
                }
            }
            response.setItems(items);
            
            // Amounts - sử dụng giá trị đã tính lại
            response.setSubtotal(subtotal);
            
            // Calculate shipping fee dynamically (for old orders that have shippingFee = 0)
            Long shippingFee = order.getShippingFee() != null ? order.getShippingFee().longValue() : 0L;
            if (shippingFee == 0L && subtotal > 0L) {
                // Build address JSON for zone detection
                String addressJson = String.format("{\"city\":\"%s\",\"district\":\"%s\"}", 
                    shippingCity != null ? shippingCity : "",
                    shippingDistrict != null ? shippingDistrict : "");
                
                var shippingResult = shippingCalculatorService.calculateFromOrderItems(
                    addressJson,
                    order.getOrderItems() != null ? new java.util.ArrayList<>(order.getOrderItems()) : new java.util.ArrayList<>(),
                    java.math.BigDecimal.valueOf(subtotal)
                );
                shippingFee = shippingResult.getShippingFee().longValue();
                
                // Update order with calculated shipping fee
                order.setShippingFee(java.math.BigDecimal.valueOf(shippingFee));
                order.setFinalAmount(java.math.BigDecimal.valueOf(subtotal + shippingFee - (order.getDiscountAmount() != null ? order.getDiscountAmount().longValue() : 0L)));
                orderRepository.save(order);
                log.info("Calculated and saved shipping fee {} for order {}", shippingFee, orderId);
            }
            
            response.setShippingFee(shippingFee);
            response.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount().longValue() : 0L);
            response.setFinalAmount(subtotal + shippingFee - (order.getDiscountAmount() != null ? order.getDiscountAmount().longValue() : 0L));
            
            // Additional
            response.setOrderNote(order.getNote());
            response.setCreatedAt(order.getCreatedAt());
            response.setMessage("Order payment details retrieved successfully");
            
            log.info("Retrieved payment details for order: {} - User: {}, Items: {}, Amount: {}, ShippingFee: {}", 
                orderId, 
                response.getUserName(),
                response.getItems().size(),
                response.getFinalAmount(),
                response.getShippingFee());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error retrieving order payment details for order: " + orderId, e);
            response.setMessage("Error: " + e.getMessage());
            return response;
        }
    }

    /**
     * Load Order data from database with eager loading of relationships
     * CRITICAL: Order.user and Order.orderItems use LAZY fetch - must load within transaction
     */
    @Transactional(readOnly = true)
    private Order loadOrderData(String orderId) {
        try {
            log.debug("Loading order data for orderId: {}", orderId);
            
            // Try UUID first
            java.util.UUID uuid = java.util.UUID.fromString(orderId);
            log.debug("Converted orderId to UUID: {}", uuid);
            
            Order order = orderRepository.findById(uuid).orElse(null);
            log.debug("Order found in database: {}", order != null);
            
            if (order != null) {
                // Force initialize lazy-loaded relationships while transaction is active
                // This MUST happen before method returns, otherwise LazyInitializationException occurs
                
                // Load User data
                try {
                    if (order.getUser() != null) {
                        log.debug("Loading user data for order");
                        order.getUser().getId();
                        order.getUser().getEmail();
                        order.getUser().getFullName();
                        order.getUser().getPhone();
                        log.debug("Loaded User: {}", order.getUser().getId());
                    } else {
                        log.warn("Order {} has no user association", orderId);
                    }
                } catch (Exception e) {
                    log.error("Error loading user data", e);
                    throw e;
                }
                
                // Load OrderItems and Product details
                try {
                    if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                        log.debug("Loading {} order items", order.getOrderItems().size());
                        for (var item : order.getOrderItems()) {
                            // Load Product details for each item
                            if (item.getProduct() != null) {
                                log.debug("Loading product data for item");
                                item.getProduct().getId();
                                item.getProduct().getName();
                                item.getProduct().getPrice();
                                log.debug("Loaded Product: {} - {}", item.getProduct().getId(), item.getProduct().getName());
                            } else {
                                log.warn("OrderItem has no product association");
                            }
                        }
                        log.debug("Loaded {} OrderItems", order.getOrderItems().size());
                    } else {
                        log.debug("Order has no items");
                    }
                } catch (Exception e) {
                    log.error("Error loading order items or products", e);
                    throw e;
                }
                
                log.info("Successfully loaded Order: {} with User: {} and {} items", 
                    order.getId(), 
                    order.getUser() != null ? order.getUser().getId() : "null",
                    order.getOrderItems() != null ? order.getOrderItems().size() : 0);
            } else {
                log.warn("Order not found in database: {}", orderId);
            }
            
            return order;
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format for orderId: {}", orderId, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error loading order data for orderId: {}", orderId, e);
            throw e;
        }
    }

/**
     * Check payment status
     */
    @Transactional(readOnly = true)
    public PaymentDTO.CheckPaymentStatusResponse checkPaymentStatus(String transactionId, PaymentMethod paymentMethod) {
        log.info("Checking payment status: transactionId={}, method={}", transactionId, paymentMethod);
        
        PaymentDTO.CheckPaymentStatusResponse response = new PaymentDTO.CheckPaymentStatusResponse();
        
        PaymentTransaction transaction = paymentTransactions.get(transactionId);
        if (transaction == null) {
            response.setTransactionId(transactionId);
            response.setStatus("not_found");
            response.setMessage("Transaction not found");
            return response;
        }
        
        response.setTransactionId(transactionId);
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus());
        response.setMessage("Payment status: " + transaction.getStatus());
        
        return response;
    }

    /**
     * Check ZaloPay payment status - STUB
     */
    private PaymentDTO.CheckPaymentStatusResponse checkZaloPayStatus(
            String appTransId,
            PaymentDTO.CheckPaymentStatusResponse response,
            PaymentTransaction transaction) {
        response.setStatus(transaction.getStatus());
        response.setMessage("ZaloPay status check not implemented");
        return response;
    }

    /**
     * Check SePay payment status - STUB
     */
    private PaymentDTO.CheckPaymentStatusResponse checkSepayStatus(
            String transactionId,
            PaymentDTO.CheckPaymentStatusResponse response,
            PaymentTransaction transaction) {
        response.setStatus(transaction.getStatus());
        response.setMessage("SePay status check not implemented");
        return response;
    }

    /**
     * Get payment history for a user
     */
    @Transactional(readOnly = true)
    public PaymentDTO.GetPaymentHistoryResponse getPaymentHistory(String userId, Integer limit, Integer offset) {
        PaymentDTO.GetPaymentHistoryResponse response = new PaymentDTO.GetPaymentHistoryResponse();
        
        List<PaymentDTO.PaymentHistoryItem> history = paymentTransactions.values().stream()
            .skip(offset)
            .limit(limit)
            .map(this::mapToHistoryItem)
            .collect(Collectors.toList());
        
        response.setHistory(history);
        response.setTotalCount(paymentTransactions.size());
        
        return response;
    }

    /**
     * Cancel a payment
     */
    @Transactional
    public PaymentDTO.CancelPaymentResponse cancelPayment(PaymentDTO.CancelPaymentRequest request) {
        log.info("Cancelling payment: transactionId={}, reason={}", request.getTransactionId(), request.getReason());
        
        PaymentDTO.CancelPaymentResponse response = new PaymentDTO.CancelPaymentResponse();
        
        PaymentTransaction transaction = paymentTransactions.get(request.getTransactionId());
        if (transaction == null) {
            response.setTransactionId(request.getTransactionId());
            response.setStatus("not_found");
            response.setMessage("Transaction not found");
            return response;
        }
        
        if ("completed".equals(transaction.getStatus())) {
            response.setTransactionId(request.getTransactionId());
            response.setStatus("cannot_cancel");
            response.setMessage("Cannot cancel completed payment");
            return response;
        }
        
        transaction.setStatus("cancelled");
        response.setTransactionId(request.getTransactionId());
        response.setStatus("cancelled");
        response.setMessage("Payment cancelled: " + request.getReason());
        
        return response;
    }

    /**
     * Get payment statistics
     */
    @Transactional(readOnly = true)
    public PaymentDTO.PaymentStatisticsResponse getPaymentStatistics() {
        PaymentDTO.PaymentStatisticsResponse response = new PaymentDTO.PaymentStatisticsResponse();
        
        long totalTransactions = paymentTransactions.size();
        long successfulTransactions = paymentTransactions.values().stream()
            .filter(t -> "completed".equals(t.getStatus()))
            .count();
        long failedTransactions = paymentTransactions.values().stream()
            .filter(t -> "failed".equals(t.getStatus()))
            .count();
        
        long totalAmount = paymentTransactions.values().stream()
            .mapToLong(PaymentTransaction::getAmount)
            .sum();
        
        long pendingAmount = paymentTransactions.values().stream()
            .filter(t -> "pending".equals(t.getStatus()))
            .mapToLong(PaymentTransaction::getAmount)
            .sum();
        
        response.setTotalTransactions(totalTransactions);
        response.setTotalAmount(totalAmount);
        response.setSuccessfulTransactions(successfulTransactions);
        response.setFailedTransactions(failedTransactions);
        response.setSuccessRate(totalTransactions > 0 ? (double) successfulTransactions / totalTransactions : 0);
        response.setPendingAmount(pendingAmount);
        
        return response;
    }

    /**
     * Resend notification for a payment
     */
    @Transactional
    public String resendNotification(String transactionId) {
        PaymentTransaction transaction = paymentTransactions.get(transactionId);
        if (transaction == null) {
            return "Transaction not found";
        }
        
        log.info("Resending notification for transaction: {}", transactionId);
        return "Notification resent for transaction: " + transactionId;
    }

    /**
     * Validate payment request
     */
    public String validatePayment(PaymentDTO.InitiatePaymentRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isEmpty()) {
            return "Order ID is required";
        }
        
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isEmpty()) {
            return "Payment method is required";
        }
        
        return "Validation successful";
    }

    /**
     * Map payment transaction to history item
     */
    private PaymentDTO.PaymentHistoryItem mapToHistoryItem(PaymentTransaction transaction) {
        PaymentDTO.PaymentHistoryItem item = new PaymentDTO.PaymentHistoryItem();
        item.setTransactionId(transaction.getTransactionId());
        item.setOrderId(transaction.getOrderId());
        item.setAmount(transaction.getAmount());
        item.setStatus(transaction.getStatus());
        item.setCreatedAt(transaction.getCreatedAt());
        item.setCompletedAt(transaction.getPaidAt());
        return item;
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "TXN_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * Internal class for storing payment transactions
     */
    private static class PaymentTransaction {
        String transactionId;
        String orderId;
        PaymentMethod paymentMethod;
        Long amount;
        String status; // pending, completed, failed, cancelled
        LocalDateTime createdAt;
        LocalDateTime paidAt;
        Order order; // Reference to actual order

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }

        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getPaidAt() { return paidAt; }
        public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
        
        public Order getOrder() { return order; }
        public void setOrder(Order order) { this.order = order; }
    }
}
