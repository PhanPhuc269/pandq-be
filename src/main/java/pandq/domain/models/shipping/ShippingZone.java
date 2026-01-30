package pandq.domain.models.shipping;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "shipping_zones")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name; // e.g., "Nội thành HCM", "Ngoại thành", "Miền Bắc"

    @Column(columnDefinition = "TEXT")
    private String cities; // JSON array: ["Hồ Chí Minh", "Bình Dương"]

    @Column(columnDefinition = "TEXT")
    private String districts; // JSON array for specific districts (optional)

    private Integer zoneLevel; // 1: Nội thành, 2: Ngoại thành, 3: Liên tỉnh, 4: Vùng xa

    @Column(nullable = false)
    private BigDecimal baseFee; // Base shipping fee (for orders <= 500g)

    @Column(nullable = false)
    private BigDecimal extraFeePerKg; // Extra fee for each additional kg

    private BigDecimal freeShipThreshold; // Minimum order amount for free shipping

    private Boolean isActive;
}
