package com.mahta.backend_gare_routiere.dto.response;

public record QrVerificationResponse(
        boolean valid,
        String message
) {
}