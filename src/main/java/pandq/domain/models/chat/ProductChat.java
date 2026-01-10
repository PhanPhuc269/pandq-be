package pandq.domain.models.chat;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pandq.domain.models.product.Product;
import pandq.domain.models.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ProductChat represents a chat conversation between a customer and admin for a specific product.
 * One customer can have multiple chats for the same product (different conversations).
 */
@Entity
@Table(name = "product_chats", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_chats_product_user", columnNames = {"product_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ProductChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatStatus status;

    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "productChat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ChatMessage> messages;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Helper methods
    public void addMessage(ChatMessage message) {
        if (this.messages == null) {
            this.messages = new java.util.ArrayList<>();
        }
        message.setProductChat(this);
        this.messages.add(message);
    }

    public void closeChat() {
        this.status = ChatStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void openChat() {
        this.status = ChatStatus.OPEN;
        this.closedAt = null;
    }

    public boolean isActive() {
        return status == ChatStatus.OPEN;
    }
}
