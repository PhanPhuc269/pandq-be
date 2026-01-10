package pandq.adapter.web.api.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ChatMessage response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {

    private String id;

    private String chatId;

    private String senderId;
    private String senderName;
    private String senderRole;
    private String senderAvatar;

    private String message;

    private String messageType;  // TEXT, IMAGE, FILE, SYSTEM

    private String imageUrl;

    // Product context - for displaying "Đang hỏi về [Product]" divider
    private String productContextId;
    private String productContextName;
    private String productContextImage;
    private String productContextPrice;

    private boolean isRead;

    private String readAt;

    private String createdAt;
}
