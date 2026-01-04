package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.PaymentDTO;
import pandq.application.services.PaymentService;
import pandq.application.port.repositories.OrderRepository;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Payment operations
 * Handles payment initiation, status checking, and order payment details retrieval
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;

    /**
     * Get payment details for an order (for checkout screen)
     * Includes: order info, user info, shipping address, items, amounts
     */
    @GetMapping("/details/{orderId}")
    public ResponseEntity<PaymentDTO.OrderPaymentDetailsResponse> getPaymentDetails(
            @PathVariable String orderId) {
        
        PaymentDTO.OrderPaymentDetailsResponse response = paymentService.getOrderPaymentDetails(orderId);
        
        if (response.getMessage() != null && response.getMessage().contains("Error")) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * DEBUG: Get all order IDs from database
     */
    @GetMapping("/orders/debug")
    public ResponseEntity<List<String>> getAllOrderIds() {
        List<String> orderIds = orderRepository.findAll().stream()
            .map(order -> order.getId().toString())
            .collect(Collectors.toList());
        return ResponseEntity.ok(orderIds);
    }

    /**
     * Initiate a payment for an order
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentDTO.InitiatePaymentResponse> initiatePayment(
            @RequestBody PaymentDTO.InitiatePaymentRequest request) {
        
        // Validate request
        String validationError = paymentService.validatePayment(request);
        if (!"Validation successful".equals(validationError)) {
            PaymentDTO.InitiatePaymentResponse errorResponse = new PaymentDTO.InitiatePaymentResponse();
            errorResponse.setStatus("failed");
            errorResponse.setMessage(validationError);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        PaymentDTO.InitiatePaymentResponse response = paymentService.initiatePayment(request);
        
        if ("failed".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check payment status
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<PaymentDTO.CheckPaymentStatusResponse> checkPaymentStatus(
            @PathVariable String transactionId,
            @RequestParam(required = false) String paymentMethod) {
        
        PaymentDTO.CheckPaymentStatusResponse response = paymentService.checkPaymentStatus(
            transactionId, 
            paymentMethod != null ? pandq.domain.models.enums.PaymentMethod.valueOf(paymentMethod) : null
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available payment methods
     */
    @GetMapping("/methods")
    public ResponseEntity<PaymentDTO.GetPaymentMethodsResponse> getPaymentMethods() {
        PaymentDTO.GetPaymentMethodsResponse response = paymentService.getPaymentMethods();
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment history for user
     */
    @GetMapping("/history")
    public ResponseEntity<PaymentDTO.GetPaymentHistoryResponse> getPaymentHistory(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        
        PaymentDTO.GetPaymentHistoryResponse response = paymentService.getPaymentHistory(userId, limit, offset);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a payment
     */
    @PostMapping("/cancel")
    public ResponseEntity<PaymentDTO.CancelPaymentResponse> cancelPayment(
            @RequestBody PaymentDTO.CancelPaymentRequest request) {
        
        PaymentDTO.CancelPaymentResponse response = paymentService.cancelPayment(request);
        
        if ("not_found".equals(response.getStatus()) || "cannot_cancel".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<PaymentDTO.PaymentStatisticsResponse> getPaymentStatistics() {
        PaymentDTO.PaymentStatisticsResponse response = paymentService.getPaymentStatistics();
        return ResponseEntity.ok(response);
    }
}
