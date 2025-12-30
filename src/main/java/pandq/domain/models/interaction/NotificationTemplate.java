package pandq.domain.models.interaction;

import jakarta.persistence.*;
import lombok.*;
import pandq.domain.models.enums.NotificationType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * NotificationTemplate - Mẫu thông báo có thể kích hoạt/không kích hoạt.
 * Admin tạo mẫu, bật/tắt, và gửi đến users khi cần.
 */
@Entity
@Table(name = "notification_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    private String targetUrl;

    /**
     * Trạng thái kích hoạt - admin có thể bật/tắt.
     * Chỉ các template đang active mới có thể gửi.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Thời điểm dự kiến gửi (optional).
     * Nếu null = gửi thủ công bởi admin.
     */
    private LocalDateTime scheduledAt;

    /**
     * Đối tượng nhận: ALL, NEW_USERS, VIP_CUSTOMERS, etc.
     */
    @Builder.Default
    private String targetAudience = "ALL";

    /**
     * Đánh dấu đã gửi lần cuối khi nào.
     */
    private LocalDateTime lastSentAt;

    /**
     * Số lần đã gửi.
     */
    @Builder.Default
    private Integer sendCount = 0;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
