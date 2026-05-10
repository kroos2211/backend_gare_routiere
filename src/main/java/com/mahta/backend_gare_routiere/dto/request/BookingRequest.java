package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull
    private Long tripId;

    @NotBlank
    private String category;

    @NotBlank
    private String seatNumber;

    private boolean hasBagage;

    private boolean prioritySeat;

    private String promoCode;

    @NotBlank
    private String tariffCategory;

    @NotBlank
    private String seatType;

    private boolean baby;
}