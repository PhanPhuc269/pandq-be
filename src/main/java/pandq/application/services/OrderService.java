package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.OrderDTO;
import pandq.application.port.repositories.OrderRepository;
import pandq.application.port.repositories.ProductRepository;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;
import pandq.domain.models.order.OrderItem;
import pandq.domain.models.product.Product;
import pandq.domain.models.user.User;
import pandq.domain.models.marketing.Promotion;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;
import pandq.infrastructure.persistence.repositories.jpa.JpaPromotionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final JpaUserRepository userRepository;
    private final AdminNotificationService adminNotificationService;
    private final ShippingCalculatorService shippingCalculatorService;
    private final VoucherService voucherService;
    private final JpaPromotionRepository promotionRepository;
    private final InventoryService inventoryService;

    @Transactional(readOnly = true)
    public List<OrderDTO.Response> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO.Response> getOrdersByUserId(String userId) {
        System.out.println("=== DEBUG: getOrdersByUserId called with userId: " + userId);

        // Try to find user by Firebase UID first
        User user = userRepository.findByFirebaseUid(userId).orElse(null);
        System.out.println("=== DEBUG: User found by Firebase UID: "
                + (user != null ? user.getId() + " - " + user.getEmail() : "null"));

        // If not found, try as UUID
        if (user == null) {
            try {
                UUID userUUID = UUID.fromString(userId);
                user = userRepository.findById(userUUID).orElse(null);
                System.out.println("=== DEBUG: User found by UUID: "
                        + (user != null ? user.getId() + " - " + user.getEmail() : "null"));
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, user not found
                System.out.println("=== DEBUG: Invalid UUID format");
                return new ArrayList<>();
            }
        }

        if (user == null) {
            System.out.println("=== DEBUG: No user found, returning empty list");
            return new ArrayList<>();
        }

        List<Order> orders = orderRepository.findByUserId(user.getId());
        System.out.println("=== DEBUG: Found " + orders.size() + " orders for user " + user.getId());
        for (Order o : orders) {
            System.out.println("=== DEBUG: Order ID: " + o.getId() + ", Status: " + o.getStatus() + ", Items: "
                    + o.getOrderItems().size());
        }

        return orders.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO.Response getOrderById(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderDTO.Response createOrder(OrderDTO.CreateRequest request) {
        User user = null;
        if (request.getUserId() != null) {
            // Try to find user by Firebase UID first
            user = userRepository.findByFirebaseUid(request.getUserId())
                    .orElse(null);

            // If not found, try as UUID
            if (user == null) {
                try {
                    UUID userId = UUID.fromString(request.getUserId());
                    user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid user ID format");
                }
            }
        } else {
            // TODO: get user from Security Context
            // For now throw error if no user ID
            throw new RuntimeException("User ID is required");
        }

        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setNote(request.getNote());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDTO.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(orderItem.getTotalPrice());
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalAmount);
        
        // Calculate shipping fee dynamically
        var shippingResult = shippingCalculatorService.calculateFromOrderItems(
                request.getShippingAddress(),
                orderItems,
                totalAmount
        );
        order.setShippingFee(shippingResult.getShippingFee());
        order.setShippingFee(shippingResult.getShippingFee());
        
        // Apply voucher if provided
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getPromotionId() != null) {
            String userIdForVoucher = user != null ? user.getId().toString() : request.getUserId();
            discountAmount = voucherService.applyVoucher(
                userIdForVoucher, 
                request.getPromotionId(), 
                totalAmount, 
                shippingResult.getShippingFee()
            );
            
            // Store the promotion reference on the order for payment callback to mark as used
            Promotion promotion = promotionRepository.findById(request.getPromotionId()).orElse(null);
            order.setPromotion(promotion);
        }
        
        order.setDiscountAmount(discountAmount);
        
        // Ensure final amount is not negative
        BigDecimal finalAmount = totalAmount.add(shippingResult.getShippingFee()).subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        order.setFinalAmount(finalAmount);

        Order savedOrder = orderRepository.save(order);
        
        // Mark voucher as used if order created successfully
        if (request.getPromotionId() != null) {
            String userIdForVoucher = user != null ? user.getId().toString() : request.getUserId();
            voucherService.markVoucherAsUsed(userIdForVoucher, request.getPromotionId());
        }
        
        // Notify admins about new order (async)
        adminNotificationService.notifyNewOrder(
                savedOrder.getId(),
                user.getFullName(),
                totalAmount
        );
        
        return mapToResponse(savedOrder);
    }

    private OrderDTO.Response mapToResponse(Order order) {
        OrderDTO.Response response = new OrderDTO.Response();
        response.setId(order.getId());
        response.setUserId(order.getUser().getId().toString());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingFee(order.getShippingFee());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setFinalAmount(order.getFinalAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setShippingAddress(order.getShippingAddress());
        response.setShippingProvider(order.getShippingProvider());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());

        // Deduplicate order items by ID (Hibernate may return duplicates due to
        // cartesian product)
        java.util.LinkedHashMap<UUID, OrderItem> uniqueItemsMap = new java.util.LinkedHashMap<>();
        for (OrderItem item : order.getOrderItems()) {
            uniqueItemsMap.putIfAbsent(item.getId(), item);
        }

        List<OrderDTO.OrderItemResponse> items = uniqueItemsMap.values().stream()
                .map(item -> {
                    OrderDTO.OrderItemResponse itemResponse = new OrderDTO.OrderItemResponse();
                    itemResponse.setProductId(item.getProduct().getId());
                    itemResponse.setProductName(item.getProduct().getName());
                    itemResponse.setQuantity(item.getQuantity());
                    itemResponse.setPrice(item.getPrice());
                    itemResponse.setTotalPrice(item.getTotalPrice());
                    // Get the first product image URL if available
                    String imageUrl = item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()
                            ? item.getProduct().getImages().get(0).getImageUrl()
                            : null;
                    itemResponse.setImageUrl(imageUrl);
                    return itemResponse;
                })
                .collect(Collectors.toList());
        response.setItems(items);

        return response;
    }

    /**
     * Apply a promotion/voucher to an existing order before payment
     */
    @Transactional
    public OrderDTO.Response applyPromotionToOrder(UUID orderId, OrderDTO.ApplyPromotionRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        // Validate and apply the voucher
        if (request.getPromotionId() != null && request.getUserId() != null) {
            BigDecimal discountAmount = voucherService.applyVoucher(
                request.getUserId(),
                request.getPromotionId(),
                order.getTotalAmount(),
                order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO
            );
            
            // Link the promotion to the order
            Promotion promotion = promotionRepository.findById(request.getPromotionId()).orElse(null);
            order.setPromotion(promotion);
            order.setDiscountAmount(discountAmount);
            
            // Recalculate final amount
            BigDecimal shippingFee = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
            BigDecimal finalAmount = order.getTotalAmount().add(shippingFee).subtract(discountAmount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
                finalAmount = BigDecimal.ZERO;
            }
            order.setFinalAmount(finalAmount);
            
            orderRepository.save(order);
        }
        
        return mapToResponse(order);
    }

    @Transactional
    public OrderDTO.Response addToCart(OrderDTO.AddToCartRequest request) {
        // Try to find user by Firebase UID first
        User user = userRepository.findByFirebaseUid(request.getUserId())
                .orElse(null);

        // If not found, try as UUID
        if (user == null) {
            try {
                UUID userId = UUID.fromString(request.getUserId());
                user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        Product product = productRepository.findById(UUID.fromString(request.getProductId()))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Find or create a PENDING order (cart) for the user
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        Order cart;

        if (pendingOrders.isEmpty()) {
            // Create new cart
            cart = new Order();
            cart.setUser(user);
            cart.setStatus(OrderStatus.PENDING);
            cart.setCreatedAt(LocalDateTime.now());
            cart.setTotalAmount(BigDecimal.ZERO);
            cart.setShippingFee(BigDecimal.ZERO);
            cart.setDiscountAmount(BigDecimal.ZERO);
            cart.setFinalAmount(BigDecimal.ZERO);
            cart.setOrderItems(new ArrayList<>());
        } else {
            // Use existing cart
            cart = pendingOrders.get(0);
        }

        // Check if product already exists in cart
        UUID productUUID = UUID.fromString(request.getProductId());
        OrderItem existingItem = cart.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productUUID))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
        } else {
            // Add new item
            OrderItem newItem = OrderItem.builder()
                    .order(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.getOrderItems().add(newItem);
        }

        // Recalculate totals
        BigDecimal totalAmount = cart.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cart.setFinalAmount(totalAmount);

        Order savedCart = orderRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional(readOnly = true)
    public OrderDTO.Response getCart(String userId) {
        // Try to find user by Firebase UID first
        User user = userRepository.findByFirebaseUid(userId)
                .orElse(null);

        // If not found, try as UUID
        UUID userUUID = null;
        if (user == null) {
            try {
                userUUID = UUID.fromString(userId);
                user = userRepository.findById(userUUID)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            // Return empty cart
            Order emptyCart = new Order();
            emptyCart.setId(UUID.randomUUID());
            emptyCart.setUser(user);
            emptyCart.setStatus(OrderStatus.PENDING);
            emptyCart.setOrderItems(new ArrayList<>());
            emptyCart.setTotalAmount(BigDecimal.ZERO);
            emptyCart.setFinalAmount(BigDecimal.ZERO);
            emptyCart.setShippingFee(BigDecimal.ZERO);
            emptyCart.setDiscountAmount(BigDecimal.ZERO);
            return mapToResponse(emptyCart);
        }
        return mapToResponse(pendingOrders.get(0));
    }

    @Transactional
    public OrderDTO.Response decreaseQuantity(OrderDTO.AddToCartRequest request) {
        User user = userRepository.findByFirebaseUid(request.getUserId())
                .orElse(null);

        UUID userUUID = null;
        if (user == null) {
            try {
                userUUID = UUID.fromString(request.getUserId());
                user = userRepository.findById(userUUID)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            throw new RuntimeException("Cart not found");
        }

        Order cart = pendingOrders.get(0);
        UUID productUUID = UUID.fromString(request.getProductId());
        Product product = productRepository.findById(productUUID)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        OrderItem existingItem = cart.getOrderItems().stream()
                .filter(item -> item.getProduct().getId().equals(productUUID))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            if (existingItem.getQuantity() > 1) {
                existingItem.setQuantity(existingItem.getQuantity() - 1);
                existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            } else {
                cart.getOrderItems().remove(existingItem);
            }
        }

        // Recalculate totals
        BigDecimal totalAmount = cart.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cart.setFinalAmount(totalAmount);

        Order savedCart = orderRepository.save(cart);
        return mapToResponse(savedCart);
    }

    @Transactional
    public OrderDTO.Response removeFromCart(String userId, UUID productId) {
        User user = userRepository.findByFirebaseUid(userId)
                .orElse(null);

        if (user == null) {
            try {
                UUID userUUID = UUID.fromString(userId);
                user = userRepository.findById(userUUID)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        if (pendingOrders.isEmpty()) {
            throw new RuntimeException("Cart not found");
        }

        Order cart = pendingOrders.get(0);
        cart.getOrderItems().removeIf(item -> item.getProduct().getId().equals(productId));

        // Recalculate totals
        BigDecimal totalAmount = cart.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(totalAmount);
        cart.setFinalAmount(totalAmount);

        Order savedCart = orderRepository.save(cart);
        return mapToResponse(savedCart);
    }

    /**
     * Merge guest cart items into user's cart after login
     * @param userId - User ID (Firebase UID or UUID string)
     * @param guestCartItems - Items from guest cart
     * @return - Merged cart with all items
     */
    @Transactional
    public OrderDTO.Response mergeGuestCart(String userId, List<OrderDTO.AddToCartRequest> guestCartItems) {
        // Find user
        User user = userRepository.findByFirebaseUid(userId)
                .orElse(null);

        if (user == null) {
            try {
                UUID userUUID = UUID.fromString(userId);
                user = userRepository.findById(userUUID)
                        .orElseThrow(() -> new RuntimeException("User not found"));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid user ID format");
            }
        }

        // Get or create user's cart
        List<Order> pendingOrders = orderRepository.findByUserIdAndStatus(user.getId(), OrderStatus.PENDING);
        Order userCart;

        if (pendingOrders.isEmpty()) {
            // Create new cart for user
            userCart = new Order();
            userCart.setUser(user);
            userCart.setStatus(OrderStatus.PENDING);
            userCart.setCreatedAt(LocalDateTime.now());
            userCart.setTotalAmount(BigDecimal.ZERO);
            userCart.setShippingFee(BigDecimal.ZERO);
            userCart.setDiscountAmount(BigDecimal.ZERO);
            userCart.setFinalAmount(BigDecimal.ZERO);
            userCart.setOrderItems(new ArrayList<>());
        } else {
            userCart = pendingOrders.get(0);
        }

        // Merge guest cart items into user cart
        for (OrderDTO.AddToCartRequest guestItem : guestCartItems) {
            // Convert productId string to UUID
            UUID productUUID;
            try {
                productUUID = UUID.fromString(guestItem.getProductId());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid product ID format: " + guestItem.getProductId());
            }

            Product product = productRepository.findById(productUUID)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + productUUID));

            // Check if product already exists in user's cart
            OrderItem existingItem = userCart.getOrderItems().stream()
                    .filter(item -> item.getProduct().getId().equals(productUUID))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                // Update quantity - add guest quantity to existing quantity
                existingItem.setQuantity(existingItem.getQuantity() + guestItem.getQuantity());
                existingItem.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            } else {
                // Add new item from guest cart
                OrderItem newItem = OrderItem.builder()
                        .order(userCart)
                        .product(product)
                        .quantity(guestItem.getQuantity())
                        .price(product.getPrice())
                        .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(guestItem.getQuantity())))
                        .build();
                userCart.getOrderItems().add(newItem);
            }
        }

        // Recalculate totals
        BigDecimal totalAmount = userCart.getOrderItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        userCart.setTotalAmount(totalAmount);
        userCart.setFinalAmount(totalAmount);

        Order savedCart = orderRepository.save(userCart);
        return mapToResponse(savedCart);
    }

    // ==================== Shipping Management ====================

    /**
     * Lấy danh sách đơn hàng theo trạng thái (cho màn hình quản lý vận chuyển)
     */
    @Transactional(readOnly = true)
    public List<OrderDTO.Response> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            // Return all orders if status is "ALL"
            if ("ALL".equalsIgnoreCase(status)) {
                return getAllOrders();
            }
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    /**
     * Gán đơn vị vận chuyển cho đơn hàng
     */
    @Transactional
    public OrderDTO.Response assignCarrier(UUID orderId, OrderDTO.AssignCarrierRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setShippingProvider(request.getShippingProvider());
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isEmpty()) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        // Auto-update status to CONFIRMED if it was PENDING
        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
        }

        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }

    /**
     * Cập nhật trạng thái vận chuyển
     */
    @Transactional
    public OrderDTO.Response updateShippingStatus(UUID orderId, OrderDTO.UpdateStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(request.getStatus());
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }
    
    /**
     * Update order status and handle inventory accordingly
     * - CONFIRMED: Reserve inventory (increase reservedQuantity)
     * - DELIVERED/COMPLETED: Decrease actual stock and reserved quantity
     * - CANCELLED: Release reserved inventory
     */
    @Transactional
    public OrderDTO.Response updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        OrderStatus currentStatus = order.getStatus();
        
        // Update inventory based on status transition
        if (newStatus == OrderStatus.CONFIRMED && currentStatus == OrderStatus.PENDING) {
            // Order confirmed - reserve inventory
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.reserveInventoryForOrder(
                    item.getProduct().getId(),
                    item.getQuantity()
                );
            }
        } else if ((newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.COMPLETED) 
                   && currentStatus != OrderStatus.DELIVERED && currentStatus != OrderStatus.COMPLETED) {
            // Order delivered/completed - decrease actual stock
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.completeOrderInventory(
                    item.getProduct().getId(),
                    item.getQuantity()
                );
            }
        } else if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
            // Order cancelled - release reserved inventory
            for (OrderItem item : order.getOrderItems()) {
                inventoryService.cancelOrderInventory(
                    item.getProduct().getId(),
                    item.getQuantity()
                );
            }
        }
        
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);
        return mapToResponse(savedOrder);
    }
}
