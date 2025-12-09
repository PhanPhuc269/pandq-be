package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.BranchStatus;
import java.util.UUID;

public class BranchDTO {

    @Data
    public static class CreateRequest {
        private String name;
        private String address;
        private String phone;
        private Double latitude;
        private Double longitude;
        private String openingHours;
        private BranchStatus status;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String address;
        private String phone;
        private Double latitude;
        private Double longitude;
        private String openingHours;
        private BranchStatus status;
    }

    @Data
    public static class Response {
        private UUID id;
        private String name;
        private String address;
        private String phone;
        private Double latitude;
        private Double longitude;
        private String openingHours;
        private BranchStatus status;
    }
}
