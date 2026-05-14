package com.mahta.backend_gare_routiere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    /*
     * =========================
     * IDENTIFIANTS
     * =========================
     */

    private Long id;

    private UUID userId;

    private Long tripId;

    /*
     * =========================
     * TRAJET GLOBAL
     * =========================
     */

    private String departureCity;

    private String arrivalCity;

    private LocalDateTime departureTime;

    /*
     * =========================
     * SEGMENT RÉSERVÉ
     * =========================
     */

    private String boardingCity;

    private String dropoffCity;

    private LocalDateTime segmentDepartureTime;

    private LocalDateTime segmentArrivalTime;

    /*
     * =========================
     * PRIX
     * =========================
     */

    private double totalPrice;

    private Double paidAmount;

    private Double pendingExtraAmount;

    private Double creditAmount;

    private String creditPromoCode;

    /*
     * =========================
     * STATUS
     * =========================
     */

    private String status;

    /*
     * =========================
     * MODIFICATION
     * =========================
     */

    private boolean modificationUsed;

    private Integer modificationCount;

    /*
     * =========================
     * PENDING MODIFICATION
     * =========================
     */

    private Long pendingTripId;

    private String pendingBoardingCity;

    private String pendingDropoffCity;

    private Double pendingTotalPrice;

    /*
     * =========================
     * DATES
     * =========================
     */

    private Instant createdAt;
}