package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequests {

    @Data
    public static class LoginRequest {

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 8, max = 72)
        private String password;
    }

    @Data
    public static class RegisterRequest {

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 8, max = 72)
        private String password;

        @NotBlank
        @Size(max = 100)
        private String firstName;

        @NotBlank
        @Size(max = 100)
        private String lastName;

        @Size(max = 20)
        private String phone;

        @Size(max = 20)
        private String cin;

        @Size(max = 5)
        private String preferredLang = "fr";
    }

    @Data
    public static class RefreshTokenRequest {

        @NotBlank
        private String refreshToken;
    }

    @Data
    public static class ChangePasswordRequest {

        @NotBlank
        private String currentPassword;

        @NotBlank
        @Size(min = 8, max = 72)
        private String newPassword;
    }
}