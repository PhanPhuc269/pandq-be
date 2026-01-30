package pandq.domain.models.interaction;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.user.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    private Boolean enablePromotions = true;

    @Builder.Default
    private Boolean enableOrders = true;

    @Builder.Default
    private Boolean enableSystem = true;

    @Builder.Default
    private Boolean enableChat = true;
}
