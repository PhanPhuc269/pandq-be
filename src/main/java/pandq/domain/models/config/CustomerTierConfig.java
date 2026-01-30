package pandq.domain.models.config;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.enums.CustomerTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Configuration for customer tier thresholds.
 * Allows admin to modify spending thresholds for each tier.
 */
@Entity
@Table(name = "customer_tier_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTierConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private CustomerTier tier;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minSpent;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxSpent; // null for PLATINUM (no upper limit)

    private String displayName;

    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime updatedAt;

    private String updatedBy;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
