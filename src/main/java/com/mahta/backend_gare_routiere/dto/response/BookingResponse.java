package com.mahta.backend_gare_routiere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;

    private UUID userId;

    private Long tripId;

    private String departureCity;

    private String arrivalCity;

    private double totalPrice;

    private String status;

    private Double pendingExtraAmount;

    private Double creditAmount;

    private String creditPromoCode;

    private Instant createdAt;
}