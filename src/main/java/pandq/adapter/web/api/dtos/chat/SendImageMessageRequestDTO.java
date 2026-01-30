package pandq.adapter.web.api.dtos.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending a message with an image URL.
 * Used when image is uploaded to Cloudinary first, then URL is sent with message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendImageMessageRequestDTO {

    private String message;

    private String imageUrl;

    @Default
    private String messageType = "IMAGE";
}
