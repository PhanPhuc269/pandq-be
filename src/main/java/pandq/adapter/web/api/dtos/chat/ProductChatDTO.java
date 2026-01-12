package pandq.adapter.web.api.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ProductChat response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductChatDTO {

    private String id;

    private String productId;
    private String productName;
    private String productImage;
    private String productPrice;

    private String customerId;
    private String customerName;
    private String customerAvatar;

    private String adminId;
    private String adminName;

    private String subject;

    private String status;  // PENDING, OPEN, CLOSED

    private int messageCount;

    private long unreadCount;

    private String createdAt;

    private String updatedAt;

    private String closedAt;

    private String lastMessageAt;

    private String lastMessagePreview;
    
    private String lastMessageSenderRole;  // ADMIN or CUSTOMER - to show "Bạn: " prefix

    private java.util.List<ChatMessageDTO> messages;
}
