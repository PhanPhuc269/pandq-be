package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReviewDTO {

    @Data
    public static class CreateRequest {
        private UUID userId; // Optional if from context
        private UUID productId;
        private Integer rating;
        private String comment;
        private List<String> imageUrls;
    }

    @Data
    public static class UpdateRequest {
        private Integer rating;
        private String comment;
        private List<String> imageUrls;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID userId;
        private String userName;
        private String userAvatar;
        private UUID productId;
        private Integer rating;
        private String comment;
        private List<String> imageUrls;
        private LocalDateTime createdAt;
    }
}
