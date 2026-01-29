package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
import java.util.UUID;

public class UserDTO {

    @Data
    public static class CreateRequest {
        private String email;
        private String fullName;
        private String phone;
        private String avatarUrl;
        private Role role;
    }

    @Data
    public static class UpdateRequest {
        private String fullName;
        private String phone;
        private String avatarUrl;
    }

    @Data
    public static class Response {
        private UUID id;
        private String email;
        private String fullName;
        private String phone;
        private String avatarUrl;
        private Role role;
        private UserStatus status;
    }

    @Data
    public static class FcmTokenRequest {
        private UUID userId;
        private String fcmToken;
    }

    @Data
    public static class FcmTokenByEmailRequest {
        private String email;
        private String fcmToken;
        private String firebaseUid;
    }

    @Data
    public static class CloseAccountRequest {
        private String email;
        private String reason;
    }
}

