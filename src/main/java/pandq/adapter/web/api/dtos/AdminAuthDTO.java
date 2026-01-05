package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.models.enums.Role;

/**
 * DTOs for admin authentication endpoints.
 */
public class AdminAuthDTO {

    /**
     * Response for admin verification endpoint.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerifyAdminResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("isAdmin")
        private boolean isAdmin;
        private String message;
        private UserInfo user;
    }

    /**
     * User information included in verification response.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String fullName;
        private String avatarUrl;
        private Role role;
    }
}
