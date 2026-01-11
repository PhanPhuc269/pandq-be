package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.PromotionDTO;
import pandq.application.services.PromotionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    public ResponseEntity<List<PromotionDTO.Response>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO.Response> getPromotionById(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.getPromotionById(id));
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<PromotionDTO.Response> getPromotionByCode(@PathVariable String code) {
        return ResponseEntity.ok(promotionService.getPromotionByCode(code));
    }

    /**
     * Validate mã giảm giá và tính số tiền giảm
     * Sử dụng cho checkout flow
     */
    @PostMapping("/validate")
    public ResponseEntity<PromotionDTO.ValidateResponse> validatePromotion(
            @RequestBody PromotionDTO.ValidateRequest request) {
        return ResponseEntity.ok(promotionService.validatePromotion(request));
    }

    @PostMapping
    public ResponseEntity<PromotionDTO.Response> createPromotion(@RequestBody PromotionDTO.CreateRequest request) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDTO.Response> updatePromotion(@PathVariable UUID id, @RequestBody PromotionDTO.UpdateRequest request) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }
}
