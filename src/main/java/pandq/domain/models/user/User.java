package pandq.domain.models.user;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
import pandq.domain.enums.CustomerTier;
import pandq.domain.enums.AccountStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String fullName;
    private String phone;
    @Column(length = 1000)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(unique = true)
    private String firebaseUid;

    @ElementCollection
    @CollectionTable(name = "user_notification_prefs", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "pref_key")
    private java.util.Set<String> notificationPreferences;

    // FCM token for push notifications
    private String fcmToken;

    // Customer tier management
    @Builder.Default
    @Column(precision = 15, scale = 2)
    private java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CustomerTier customerTier = CustomerTier.BRONZE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
