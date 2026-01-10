package pandq.adapter.web.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WebSocket chat messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketChatMessage {

    private String chatId;          // UUID of the ProductChat

    private String senderId;        // UUID of the sender (user/admin)

    private String senderName;      // Name of the sender

    private String senderAvatar;    // Avatar URL of the sender

    private String message;         // Message content

    private String messageType;     // TEXT, IMAGE, FILE, SYSTEM

    private Long timestamp;         // Message timestamp in milliseconds

    private boolean isFromAdmin;    // Flag to indicate if message is from admin

    private String messageId;       // ID of saved message from database
}
