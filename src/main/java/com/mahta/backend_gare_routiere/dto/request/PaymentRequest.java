package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull
    private Long bookingId;

    @NotBlank
    private String method;
}