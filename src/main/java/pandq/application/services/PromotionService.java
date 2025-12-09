package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.PromotionDTO;
import pandq.application.port.repositories.PromotionRepository;
import pandq.domain.models.enums.Status;
import pandq.domain.models.marketing.Promotion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;

    @Transactional(readOnly = true)
    public List<PromotionDTO.Response> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionDTO.Response getPromotionById(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        return mapToResponse(promotion);
    }
    
    @Transactional(readOnly = true)
    public PromotionDTO.Response getPromotionByCode(String code) {
        Promotion promotion = promotionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));
        return mapToResponse(promotion);
    }

    @Transactional
    public PromotionDTO.Response createPromotion(PromotionDTO.CreateRequest request) {
        Promotion promotion = Promotion.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(request.getType())
                .value(request.getValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .minOrderValue(request.getMinOrderValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .quantityLimit(request.getQuantityLimit())
                .usageCount(0)
                .applicableCategoryIds(request.getApplicableCategoryIds())
                .applicableProductIds(request.getApplicableProductIds())
                .status(Status.ACTIVE)
                .build();

        Promotion savedPromotion = promotionRepository.save(promotion);
        return mapToResponse(savedPromotion);
    }

    @Transactional
    public PromotionDTO.Response updatePromotion(UUID id, PromotionDTO.UpdateRequest request) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promotion not found"));

        promotion.setName(request.getName());
        promotion.setValue(request.getValue());
        promotion.setMaxDiscountAmount(request.getMaxDiscountAmount());
        promotion.setMinOrderValue(request.getMinOrderValue());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setQuantityLimit(request.getQuantityLimit());
        promotion.setApplicableCategoryIds(request.getApplicableCategoryIds());
        promotion.setApplicableProductIds(request.getApplicableProductIds());
        
        if (request.getStatus() != null) {
            promotion.setStatus(request.getStatus());
        }

        Promotion savedPromotion = promotionRepository.save(promotion);
        return mapToResponse(savedPromotion);
    }

    @Transactional
    public void deletePromotion(UUID id) {
        promotionRepository.deleteById(id);
    }

    private PromotionDTO.Response mapToResponse(Promotion promotion) {
        PromotionDTO.Response response = new PromotionDTO.Response();
        response.setId(promotion.getId());
        response.setCode(promotion.getCode());
        response.setName(promotion.getName());
        response.setType(promotion.getType());
        response.setValue(promotion.getValue());
        response.setMaxDiscountAmount(promotion.getMaxDiscountAmount());
        response.setMinOrderValue(promotion.getMinOrderValue());
        response.setStartDate(promotion.getStartDate());
        response.setEndDate(promotion.getEndDate());
        response.setQuantityLimit(promotion.getQuantityLimit());
        response.setUsageCount(promotion.getUsageCount());
        response.setStatus(promotion.getStatus());
        response.setApplicableCategoryIds(promotion.getApplicableCategoryIds());
        response.setApplicableProductIds(promotion.getApplicableProductIds());
        return response;
    }
}
