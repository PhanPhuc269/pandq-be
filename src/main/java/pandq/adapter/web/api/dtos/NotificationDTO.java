package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationDTO {

    @Data
    public static class Response {
        private UUID id;
        private UUID userId;
        private NotificationType type;
        private String title;
        private String body;
        private String targetUrl;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }
}
