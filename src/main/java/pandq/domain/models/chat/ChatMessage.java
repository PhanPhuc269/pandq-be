package pandq.domain.models.chat;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ChatMessage represents a single message in a ProductChat conversation.
 */
@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_chat_id", nullable = false)
    private ProductChat productChat;
    
    // Cache chat ID to avoid lazy loading issues during serialization
    @Column(name = "chat_id")
    private UUID chatId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_role", nullable = false)
    private String senderRole;  // ADMIN or CUSTOMER

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(name = "image_url")
    private String imageUrl;

    // Product context - stores which product this message is about
    @Column(name = "product_context_id")
    private UUID productContextId;

    @Column(name = "product_context_name")
    private String productContextName;

    @Column(name = "product_context_image")
    private String productContextImage;

    @Column(name = "product_context_price")
    private String productContextPrice;

    @Column(nullable = false)
    private boolean isRead;

    private LocalDateTime readAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean isFromAdmin() {
        return productChat.getAdmin() != null && productChat.getAdmin().getId().equals(senderId);
    }

    public boolean isFromCustomer() {
        return productChat.getCustomer().getId().equals(senderId);
    }
}
