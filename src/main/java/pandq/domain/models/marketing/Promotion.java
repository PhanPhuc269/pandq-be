package pandq.domain.models.marketing;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.enums.DiscountType;
import pandq.domain.models.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ElementCollection
    @CollectionTable(
        name = "promotion_applicable_categories",
        joinColumns = @JoinColumn(name = "promotion_id")
    )
    @Column(name = "applicable_category_ids")
    private java.util.List<UUID> applicableCategoryIds;

    @ElementCollection
    @CollectionTable(
        name = "promotion_applicable_products",
        joinColumns = @JoinColumn(name = "promotion_id")
    )
    @Column(name = "applicable_product_ids")
    private java.util.List<UUID> applicableProductIds;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType type;

    private BigDecimal value; // Percentage or Amount
    private BigDecimal maxDiscountAmount; // Cap for percentage
    private BigDecimal minOrderValue;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer quantityLimit;
    private Integer usageCount;

    @Enumerated(EnumType.STRING)
    private Status status;
}
