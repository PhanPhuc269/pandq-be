package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.util.UUID;

public class CategoryDTO {
    
    @Data
    public static class CreateRequest {
        private String name;
        private String description;
        private String imageUrl;
        private UUID parentId;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String description;
        private String imageUrl;
        private UUID parentId;
    }

    @Data
    public static class Response {
        private UUID id;
        private String name;
        private String description;
        private String imageUrl;
        private UUID parentId;
    }
}
