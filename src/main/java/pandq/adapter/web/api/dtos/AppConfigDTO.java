package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class AppConfigDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitConfigResponse {
        private Integer locationVersion;
        private Integer categoryVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationResponse {
        private String id;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private String id;
        private String name;
        private String iconUrl;
    }
}
