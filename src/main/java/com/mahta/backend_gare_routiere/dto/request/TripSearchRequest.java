package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TripSearchRequest {

    @NotBlank
    private String departureCity;

    @NotBlank
    private String arrivalCity;

    @NotNull
    private LocalDate date;

    // price, duration, departureTime
    private String sortBy;
}