package com.mahta.backend_gare_routiere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    /*
     * =========================
     * TRAJET
     * =========================
     */

    @NotNull
    private Long tripId;

    @NotBlank
    private String boardingCity;

    @NotBlank
    private String dropoffCity;

    /*
     * =========================
     * SIÈGE
     * =========================
     */

    @NotBlank
    private String seatNumber;

    @NotBlank
    private String seatType;

    /*
     * =========================
     * TARIFICATION
     * =========================
     */

    @NotBlank
    private String tariffCategory;

    /*
     * =========================
     * OPTIONS
     * =========================
     */

    private boolean hasBagage;

    private boolean prioritySeat;

    private boolean baby;

    /*
     * =========================
     * PROMO
     * =========================
     */

    private String promoCode;
}