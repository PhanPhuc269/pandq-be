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
                .description(request.getDescription())
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

    /**
     * Validate mã giảm giá và tính toán số tiền giảm
     */
    @Transactional(readOnly = true)
    public PromotionDTO.ValidateResponse validatePromotion(PromotionDTO.ValidateRequest request) {
        // 1. Kiểm tra mã giảm giá tồn tại
        var promotionOpt = promotionRepository.findByCode(request.getPromoCode());
        if (promotionOpt.isEmpty()) {
            return PromotionDTO.ValidateResponse.error("Mã giảm giá không tồn tại");
        }

        Promotion promotion = promotionOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 2. Kiểm tra trạng thái ACTIVE
        if (promotion.getStatus() != Status.ACTIVE) {
            return PromotionDTO.ValidateResponse.error("Mã giảm giá không còn hoạt động");
        }

        // 3. Kiểm tra thời gian hiệu lực
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return PromotionDTO.ValidateResponse.error("Mã giảm giá chưa đến thời gian áp dụng");
        }
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return PromotionDTO.ValidateResponse.error("Mã giảm giá đã hết hạn");
        }

        // 4. Kiểm tra giới hạn lượt sử dụng
        if (promotion.getQuantityLimit() != null && promotion.getUsageCount() != null
                && promotion.getUsageCount() >= promotion.getQuantityLimit()) {
            return PromotionDTO.ValidateResponse.error("Mã giảm giá đã hết lượt sử dụng");
        }

        // 5. Kiểm tra giá trị đơn hàng tối thiểu
        if (promotion.getMinOrderValue() != null && request.getOrderTotal() != null
                && request.getOrderTotal().compareTo(promotion.getMinOrderValue()) < 0) {
            return PromotionDTO.ValidateResponse.error(
                    String.format("Đơn hàng phải từ %,.0f₫ để áp dụng mã này", promotion.getMinOrderValue()));
        }

        // 6. Kiểm tra sản phẩm/danh mục áp dụng (nếu có giới hạn)
        if (promotion.getApplicableProductIds() != null && !promotion.getApplicableProductIds().isEmpty()) {
            if (request.getProductIds() == null || request.getProductIds().isEmpty()) {
                return PromotionDTO.ValidateResponse.error("Mã giảm giá chỉ áp dụng cho một số sản phẩm nhất định");
            }
            boolean hasApplicableProduct = request.getProductIds().stream()
                    .anyMatch(promotion.getApplicableProductIds()::contains);
            if (!hasApplicableProduct) {
                return PromotionDTO.ValidateResponse.error("Mã giảm giá không áp dụng cho các sản phẩm trong đơn hàng");
            }
        }

        if (promotion.getApplicableCategoryIds() != null && !promotion.getApplicableCategoryIds().isEmpty()) {
            if (request.getCategoryIds() == null || request.getCategoryIds().isEmpty()) {
                return PromotionDTO.ValidateResponse.error("Mã giảm giá chỉ áp dụng cho một số danh mục nhất định");
            }
            boolean hasApplicableCategory = request.getCategoryIds().stream()
                    .anyMatch(promotion.getApplicableCategoryIds()::contains);
            if (!hasApplicableCategory) {
                return PromotionDTO.ValidateResponse.error("Mã giảm giá không áp dụng cho các danh mục trong đơn hàng");
            }
        }

        // 7. Tính toán số tiền giảm
        java.math.BigDecimal discountAmount = calculateDiscount(promotion, request.getOrderTotal());
        java.math.BigDecimal finalAmount = request.getOrderTotal().subtract(discountAmount);
        if (finalAmount.compareTo(java.math.BigDecimal.ZERO) < 0) {
            finalAmount = java.math.BigDecimal.ZERO;
        }

        return PromotionDTO.ValidateResponse.success(discountAmount, finalAmount, mapToResponse(promotion));
    }

    /**
     * Tính số tiền giảm dựa trên loại khuyến mãi
     */
    private java.math.BigDecimal calculateDiscount(Promotion promotion, java.math.BigDecimal orderTotal) {
        if (promotion.getValue() == null || orderTotal == null) {
            return java.math.BigDecimal.ZERO;
        }

        java.math.BigDecimal discount;
        switch (promotion.getType()) {
            case PERCENTAGE:
                // Giảm theo phần trăm
                discount = orderTotal.multiply(promotion.getValue())
                        .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
                // Áp dụng giới hạn giảm tối đa
                if (promotion.getMaxDiscountAmount() != null && discount.compareTo(promotion.getMaxDiscountAmount()) > 0) {
                    discount = promotion.getMaxDiscountAmount();
                }
                break;
            case FIXED_AMOUNT:
                // Giảm số tiền cố định
                discount = promotion.getValue();
                break;
            case FREE_SHIPPING:
                // Miễn phí vận chuyển - giả sử phí ship mặc định là giá trị của promotion
                discount = promotion.getValue() != null ? promotion.getValue() : java.math.BigDecimal.ZERO;
                break;
            default:
                discount = java.math.BigDecimal.ZERO;
        }
        return discount;
    }

    /**
     * Tăng số lượt sử dụng khuyến mãi
     */
    @Transactional
    public void incrementUsageCount(String promoCode) {
        var promotionOpt = promotionRepository.findByCode(promoCode);
        if (promotionOpt.isPresent()) {
            Promotion promotion = promotionOpt.get();
            int currentCount = promotion.getUsageCount() != null ? promotion.getUsageCount() : 0;
            promotion.setUsageCount(currentCount + 1);
            promotionRepository.save(promotion);
        }
    }

    private PromotionDTO.Response mapToResponse(Promotion promotion) {
        PromotionDTO.Response response = new PromotionDTO.Response();
        response.setId(promotion.getId());
        response.setCode(promotion.getCode());
        response.setName(promotion.getName());
        response.setDescription(promotion.getDescription());
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
