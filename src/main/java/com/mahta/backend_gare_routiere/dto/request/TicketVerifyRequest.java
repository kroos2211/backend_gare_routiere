package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TicketVerifyRequest {

    @NotBlank
    private String qrContent;
}