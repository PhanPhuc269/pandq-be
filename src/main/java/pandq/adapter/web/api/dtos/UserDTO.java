package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
import java.util.UUID;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDTO {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        @NotBlank(message = "Full name is required")
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

