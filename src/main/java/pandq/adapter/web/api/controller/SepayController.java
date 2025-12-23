package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.SepayDTO;
import pandq.application.services.SepayService;

/**
 * Controller for SePay payment integration
 */
@RestController
@RequestMapping("/api/v1/payments/sepay")
@RequiredArgsConstructor
public class SepayController {

    private final SepayService sepayService;

    /**
     * Create a VietQR code for payment
     */
    @PostMapping("/create-qr")
    public ResponseEntity<SepayDTO.CreateQRResponse> createQR(
            @RequestBody SepayDTO.CreateQRRequest request) {
        SepayDTO.CreateQRResponse response = sepayService.createQRCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Webhook endpoint for SePay to notify payment received
     * Configure this URL in SePay dashboard: https://your-domain.com/api/v1/payments/sepay/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<SepayDTO.WebhookResponse> handleWebhook(
            @RequestBody SepayDTO.WebhookRequest request) {
        SepayDTO.WebhookResponse response = sepayService.handleWebhook(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Check payment status for a transaction
     */
    @GetMapping("/status/{transactionId}")
    public ResponseEntity<SepayDTO.TransactionQueryResponse> checkStatus(
            @PathVariable String transactionId) {
        SepayDTO.TransactionQueryResponse response = sepayService.checkPaymentStatus(transactionId);
        return ResponseEntity.ok(response);
    }
}
