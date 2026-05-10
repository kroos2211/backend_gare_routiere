package com.mahta.backend_gare_routiere.dto.response;

import com.mahta.backend_gare_routiere.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

public class AuthResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {

        private String accessToken;

        private String refreshToken;

        @Builder.Default
        private String tokenType = "Bearer";

        private long expiresIn;

        private UserInfo user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {

        private UUID id;

        private String email;

        private String firstName;

        private String lastName;

        private UserRole role;

        private String preferredLang;
    }
}