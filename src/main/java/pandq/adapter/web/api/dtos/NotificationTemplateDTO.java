package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.models.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationTemplateDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private String title;
        private String body;
        private NotificationType type;
        private String targetUrl;
        private Boolean isActive;
        private LocalDateTime scheduledAt;
        private String targetAudience;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String title;
        private String body;
        private NotificationType type;
        private String targetUrl;
        private Boolean isActive;
        private LocalDateTime scheduledAt;
        private String targetAudience;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private String title;
        private String body;
        private NotificationType type;
        private String targetUrl;
        private Boolean isActive;
        private LocalDateTime scheduledAt;
        private String targetAudience;
        private LocalDateTime lastSentAt;
        private Integer sendCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
