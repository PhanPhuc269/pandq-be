package pandq.adapter.web.api.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.models.chat.MessageType;

import java.util.UUID;

/**
 * DTO for starting a new chat.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartChatRequestDTO {

    private String subject;

    public String getSubject() {
        return subject != null ? subject : "Product Inquiry";
    }
}
