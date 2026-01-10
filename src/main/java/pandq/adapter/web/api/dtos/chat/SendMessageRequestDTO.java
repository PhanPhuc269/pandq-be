package pandq.adapter.web.api.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.models.chat.MessageType;

/**
 * DTO for sending a message in a chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequestDTO {

    private String message;

    private MessageType messageType;

    // Product context - for displaying "Đang hỏi về [Product]" divider
    private String productContextId;
    private String productContextName;
    private String productContextImage;
    private String productContextPrice;

    public MessageType getMessageType() {
        return messageType != null ? messageType : MessageType.TEXT;
    }
}
