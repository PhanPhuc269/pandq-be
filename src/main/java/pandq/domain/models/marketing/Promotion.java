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
    private java.util.List<UUID> applicableCategoryIds;

    @ElementCollection
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
