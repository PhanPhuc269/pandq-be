package pandq.domain.models.product;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_specifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String specKey;

    @Column(nullable = false)
    private String specValue;
}
