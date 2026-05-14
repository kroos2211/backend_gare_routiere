package com.mahta.backend_gare_routiere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripHistoryResponse {

    private Long tripId;

    private String departureCity;

    private String arrivalCity;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private String status;

    private int availableSeats;

    private boolean finished;
}