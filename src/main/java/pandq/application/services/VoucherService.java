package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.VoucherDTO;
import pandq.domain.models.enums.Status;
import pandq.domain.models.marketing.Promotion;
import pandq.domain.models.marketing.UserVoucher;
import pandq.domain.models.user.User;
import pandq.infrastructure.persistence.repositories.jpa.JpaPromotionRepository;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserVoucherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private static final Logger log = LoggerFactory.getLogger(VoucherService.class);

    private final JpaPromotionRepository promotionRepository;
    private final JpaUserVoucherRepository userVoucherRepository;
    private final JpaUserRepository userRepository;

    /**
     * Get all available (active, not expired) vouchers
     * Also marks if user has already claimed each voucher
     */
    @Transactional(readOnly = true)
    public VoucherDTO.VoucherListResponse getAvailableVouchers(String userId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Get active promotions (including those with null end date)
        List<Promotion> promotions = promotionRepository.findActivePromotions(now).stream()
                .filter(p -> p.getQuantityLimit() == null || p.getUsageCount() == null || 
                        p.getUsageCount() < p.getQuantityLimit())
                .collect(Collectors.toList());
        
        // Get user's claimed vouchers map if logged in
        final java.util.Map<UUID, UserVoucher> claimedVouchersMap = new java.util.HashMap<>();
        if (userId != null && !userId.isEmpty()) {
            User user = findUser(userId);
            if (user != null) {
                userVoucherRepository.findByUserId(user.getId()).forEach(uv -> 
                    claimedVouchersMap.put(uv.getPromotion().getId(), uv)
                );
            }
        }
        
        List<VoucherDTO.VoucherResponse> vouchers = promotions.stream()
                .filter(p -> {
                    // Filter out vouchers that have been used by this user
                    UserVoucher uv = claimedVouchersMap.get(p.getId());
                    boolean isUsed = uv != null && uv.getIsUsed();
                    return !isUsed; // Only include vouchers that have NOT been used
                })
                .map(p -> {
                    UserVoucher uv = claimedVouchersMap.get(p.getId());
                    boolean isClaimed = uv != null;
                    boolean isUsed = uv != null && uv.getIsUsed();
                    LocalDateTime claimedAt = uv != null ? uv.getClaimedAt() : null;
                    return mapToVoucherResponse(p, isClaimed, isUsed, claimedAt);
                })
                .collect(Collectors.toList());
        
        return VoucherDTO.VoucherListResponse.builder()
                .vouchers(vouchers)
                .totalCount(vouchers.size())
                .build();
    }

    /**
     * Get user's claimed vouchers (wallet)
     */
    @Transactional(readOnly = true)
    public VoucherDTO.VoucherListResponse getMyVouchers(String userId) {
        User user = findUser(userId);
        if (user == null) {
            return VoucherDTO.VoucherListResponse.builder()
                    .vouchers(List.of())
                    .totalCount(0)
                    .build();
        }
        
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserIdAndIsUsedFalse(user.getId());
        
        List<VoucherDTO.VoucherResponse> vouchers = userVouchers.stream()
                .map(uv -> mapToVoucherResponse(uv.getPromotion(), true, uv.getIsUsed(), uv.getClaimedAt()))
                .collect(Collectors.toList());
        
        return VoucherDTO.VoucherListResponse.builder()
                .vouchers(vouchers)
                .totalCount(vouchers.size())
                .build();
    }

    /**
     * Claim (save) a voucher to user's wallet
     */
    @Transactional
    public VoucherDTO.ClaimResponse claimVoucher(String userId, VoucherDTO.ClaimRequest request) {
        User user = findUser(userId);
        if (user == null) {
            return VoucherDTO.ClaimResponse.builder()
                    .success(false)
                    .message("User not found. Please login.")
                    .build();
        }
        
        UUID promotionId = UUID.fromString(request.getPromotionId());
        Promotion promotion = promotionRepository.findById(promotionId).orElse(null);
        
        if (promotion == null) {
            return VoucherDTO.ClaimResponse.builder()
                    .success(false)
                    .message("Voucher not found")
                    .build();
        }
        
        // Check if already claimed
        if (userVoucherRepository.existsByUserIdAndPromotionId(user.getId(), promotionId)) {
            return VoucherDTO.ClaimResponse.builder()
                    .success(false)
                    .message("Bạn đã lưu voucher này rồi")
                    .voucher(mapToVoucherResponse(promotion, true, false, null))
                    .build();
        }
        
        // Check if voucher is still valid
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getEndDate() != null && promotion.getEndDate().isBefore(now)) {
            return VoucherDTO.ClaimResponse.builder()
                    .success(false)
                    .message("Voucher đã hết hạn")
                    .build();
        }
        
        // Check quantity limit
        if (promotion.getQuantityLimit() != null && promotion.getUsageCount() != null 
                && promotion.getUsageCount() >= promotion.getQuantityLimit()) {
            return VoucherDTO.ClaimResponse.builder()
                    .success(false)
                    .message("Voucher đã hết lượt sử dụng")
                    .build();
        }
        
        // Save to user's wallet
        UserVoucher userVoucher = UserVoucher.builder()
                .user(user)
                .promotion(promotion)
                .claimedAt(LocalDateTime.now())
                .isUsed(false)
                .build();
        
        userVoucherRepository.save(userVoucher);
        log.info("User {} claimed voucher {}", user.getId(), promotion.getCode());
        
        return VoucherDTO.ClaimResponse.builder()
                .success(true)
                .message("Đã lưu voucher vào ví của bạn!")
                .voucher(mapToVoucherResponse(promotion, true, false, userVoucher.getClaimedAt()))
                .build();
    }

    /**
     * Map promotion to voucher response DTO
     */
    private VoucherDTO.VoucherResponse mapToVoucherResponse(
            Promotion p, 
            boolean isClaimed, 
            boolean isUsed,
            LocalDateTime claimedAt
    ) {
        return VoucherDTO.VoucherResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .discountType(p.getType() != null ? p.getType().name() : null)
                .value(p.getValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .minOrderValue(p.getMinOrderValue())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .quantityLimit(p.getQuantityLimit())
                .usageCount(p.getUsageCount())
                .isClaimed(isClaimed)
                .isUsed(isUsed)
                .claimedAt(claimedAt)
                .build();
    }

    /**
     * Find user by Firebase UID or UUID
     */
    /**
     * Validate and calculate voucher discount
     */
    @Transactional(readOnly = true)
    public java.math.BigDecimal applyVoucher(String userId, UUID promotionId, java.math.BigDecimal orderTotal, java.math.BigDecimal shippingFee) {
        User user = findUser(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        UserVoucher userVoucher = userVoucherRepository.findByUserIdAndPromotionId(user.getId(), promotionId)
                .orElseThrow(() -> new RuntimeException("Voucher not found in your wallet"));

        if (userVoucher.getIsUsed()) {
            throw new RuntimeException("Voucher has already been used");
        }

        Promotion p = userVoucher.getPromotion();
        LocalDateTime now = LocalDateTime.now();

        if (p.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("Voucher is not active");
        }
        
        if (p.getStartDate() != null && p.getStartDate().isAfter(now)) {
             throw new RuntimeException("Voucher is not yet active");
        }

        if (p.getEndDate() != null && p.getEndDate().isBefore(now)) {
            throw new RuntimeException("Voucher has expired");
        }

        if (p.getMinOrderValue() != null && orderTotal.compareTo(p.getMinOrderValue()) < 0) {
            throw new RuntimeException("Order total does not meet minimum requirement: " + p.getMinOrderValue());
        }

        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;

        if (p.getType() == pandq.domain.models.enums.DiscountType.FREE_SHIPPING) {
            // Discount amount is the shipping fee, up to maxDiscountAmount if set
            discount = shippingFee;
            if (p.getMaxDiscountAmount() != null && discount.compareTo(p.getMaxDiscountAmount()) > 0) {
                discount = p.getMaxDiscountAmount();
            }
        } else if (p.getType() == pandq.domain.models.enums.DiscountType.PERCENTAGE) {
            // Value is percentage (e.g. 10 for 10%)
            discount = orderTotal.multiply(p.getValue()).divide(java.math.BigDecimal.valueOf(100));
            if (p.getMaxDiscountAmount() != null && discount.compareTo(p.getMaxDiscountAmount()) > 0) {
                discount = p.getMaxDiscountAmount();
            }
        } else {
            // Fixed amount
            discount = p.getValue();
        }
        
        return discount;
    }

    /**
     * Mark voucher as used after successful order creation
     */
    @Transactional
    public void markVoucherAsUsed(String userId, UUID promotionId) {
        log.info("Marking voucher {} as used for user {}", promotionId, userId);
        
        User user = findUser(userId);
        if (user == null) {
            log.error("User not found when marking voucher used: {}", userId);
            throw new RuntimeException("User not found");
        }
        
        UserVoucher userVoucher = userVoucherRepository.findByUserIdAndPromotionId(user.getId(), promotionId)
                .orElseThrow(() -> {
                    log.error("Voucher {} not found for user {}", promotionId, user.getId());
                    return new RuntimeException("Voucher not found");
                });
        
        if (userVoucher.getIsUsed()) {
            log.warn("Voucher {} was already marked as used for user {}", promotionId, user.getId());
        }
        
        userVoucher.setIsUsed(true);
        userVoucher.setUsedAt(LocalDateTime.now());
        
        // Also increment global usage count
        Promotion p = userVoucher.getPromotion();
        if (p.getUsageCount() == null) p.setUsageCount(0);
        p.setUsageCount(p.getUsageCount() + 1);
        promotionRepository.save(p);
        
        userVoucherRepository.save(userVoucher);
        userVoucherRepository.flush(); // Force flush
        
        log.info("Successfully marked voucher {} as used for user {}", promotionId, user.getId());
    }

    private User findUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return null;
        }
        
        // Try Firebase UID first
        User user = userRepository.findByFirebaseUid(userId).orElse(null);
        
        // Try UUID if not found
        if (user == null) {
            try {
                UUID uuid = UUID.fromString(userId);
                user = userRepository.findById(uuid).orElse(null);
            } catch (IllegalArgumentException e) {
                // Not a valid UUID
            }
        }
        
        return user;
    }
}
