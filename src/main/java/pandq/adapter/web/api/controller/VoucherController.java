package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.VoucherDTO;
import pandq.application.services.VoucherService;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    /**
     * Get all available vouchers (can be accessed without login)
     * If userId is provided, also marks which vouchers user has already claimed
     */
    @GetMapping("/available")
    public ResponseEntity<VoucherDTO.VoucherListResponse> getAvailableVouchers(
            @RequestParam(required = false) String userId
    ) {
        return ResponseEntity.ok(voucherService.getAvailableVouchers(userId));
    }

    /**
     * Get user's claimed vouchers (wallet)
     */
    @GetMapping("/my-wallet")
    public ResponseEntity<VoucherDTO.VoucherListResponse> getMyVouchers(
            @RequestParam String userId
    ) {
        return ResponseEntity.ok(voucherService.getMyVouchers(userId));
    }

    /**
     * Claim (save) a voucher to user's wallet
     */
    @PostMapping("/claim")
    public ResponseEntity<VoucherDTO.ClaimResponse> claimVoucher(
            @RequestParam String userId,
            @RequestBody VoucherDTO.ClaimRequest request
    ) {
        return ResponseEntity.ok(voucherService.claimVoucher(userId, request));
    }
}
