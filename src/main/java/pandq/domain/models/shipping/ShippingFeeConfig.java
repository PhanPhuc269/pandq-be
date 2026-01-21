package pandq.domain.models.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shipping_fee_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingFeeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private ShippingZone zone;

    private Integer minWeight; // Minimum weight in grams

    private Integer maxWeight; // Maximum weight in grams

    @Column(nullable = false)
    private BigDecimal fee; // Shipping fee for this weight range

    private Boolean isActive;
}
