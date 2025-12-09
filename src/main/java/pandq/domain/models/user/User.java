package pandq.domain.models.user;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
