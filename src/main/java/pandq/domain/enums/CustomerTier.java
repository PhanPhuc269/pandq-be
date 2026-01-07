package pandq.domain.enums;

/**
 * Customer tier levels based on total spending.
 * Tiers determine customer loyalty benefits and recognition.
 */
public enum CustomerTier {
    /**
     * Bronze tier: 0đ - 10,000,000đ
     */
    BRONZE,

    /**
     * Silver tier: 10,000,000đ - 50,000,000đ
     */
    SILVER,

    /**
     * Gold tier: 50,000,000đ - 200,000,000đ
     */
    GOLD,

    /**
     * Platinum tier: > 200,000,000đ
     */
    PLATINUM;

    /**
     * Determine tier based on total spent amount.
     * 
     * @param totalSpent Total amount customer has spent
     * @return Appropriate CustomerTier
     */
    public static CustomerTier fromTotalSpent(java.math.BigDecimal totalSpent) {
        if (totalSpent == null) {
            return BRONZE;
        }

        java.math.BigDecimal amount = totalSpent;

        if (amount.compareTo(new java.math.BigDecimal("200000000")) > 0) {
            return PLATINUM;
        } else if (amount.compareTo(new java.math.BigDecimal("50000000")) > 0) {
            return GOLD;
        } else if (amount.compareTo(new java.math.BigDecimal("10000000")) > 0) {
            return SILVER;
        } else {
            return BRONZE;
        }
    }
}
