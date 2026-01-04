package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.ZaloPayDTO;
import pandq.application.services.ZaloPayService;

/**
 * Controller for ZaloPay payment integration
 */
@RestController
@RequestMapping("/api/v1/payments/zalopay")
@RequiredArgsConstructor
public class ZaloPayController {

    private final ZaloPayService zaloPayService;

    /**
     * Create a ZaloPay payment order
     */
    @PostMapping("/create-order")
    public ResponseEntity<ZaloPayDTO.CreateOrderResponse> createOrder(
            @RequestBody ZaloPayDTO.CreateOrderRequest request) {
        ZaloPayDTO.CreateOrderResponse response = zaloPayService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Callback endpoint for ZaloPay to notify payment result
     */
    @PostMapping("/callback")
    public ResponseEntity<ZaloPayDTO.CallbackResponse> handleCallback(
            @RequestBody ZaloPayDTO.CallbackRequest request) {
        
        // Verify callback MAC
        boolean isValid = zaloPayService.verifyCallback(request.getData(), request.getMac());
        
        ZaloPayDTO.CallbackResponse response = new ZaloPayDTO.CallbackResponse();
        if (isValid) {
            // Process callback and update order status
            zaloPayService.processCallback(request.getData());
            response.setReturnCode(1);
            response.setReturnMessage("Success");
        } else {
            response.setReturnCode(-1);
            response.setReturnMessage("Invalid MAC");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Query payment status
     */
    @GetMapping("/status/{appTransId}")
    public ResponseEntity<ZaloPayDTO.QueryStatusResponse> queryStatus(
            @PathVariable String appTransId) {
        ZaloPayDTO.QueryStatusResponse response = zaloPayService.queryStatus(appTransId);
        return ResponseEntity.ok(response);
    }
}
