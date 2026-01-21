package pandq.domain.models.marketing;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_vouchers", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "promotion_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false)
    private LocalDateTime claimedAt;

    private LocalDateTime usedAt; // null if not used yet

    @Builder.Default
    private Boolean isUsed = false;
}
