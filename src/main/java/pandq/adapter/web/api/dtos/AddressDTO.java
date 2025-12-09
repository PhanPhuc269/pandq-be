package pandq.adapter.web.api.dtos;

import lombok.Data;
import java.util.UUID;

public class AddressDTO {

    @Data
    public static class CreateRequest {
        private UUID userId;
        private String receiverName;
        private String phone;
        private String detailAddress;
        private String ward;
        private String district;
        private String city;
        private Boolean isDefault;
    }

    @Data
    public static class UpdateRequest {
        private String receiverName;
        private String phone;
        private String detailAddress;
        private String ward;
        private String district;
        private String city;
        private Boolean isDefault;
    }

    @Data
    public static class Response {
        private UUID id;
        private UUID userId;
        private String receiverName;
        private String phone;
        private String detailAddress;
        private String ward;
        private String district;
        private String city;
        private Boolean isDefault;
    }
}
