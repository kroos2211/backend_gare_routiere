package com.mahta.backend_gare_routiere.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverDashboardResponse {

    private Long tripId;

    private String departureCity;

    private String arrivalCity;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private String status;

    private int availableSeats;

    private boolean finished;

    private boolean scanClosed;

    private boolean canScan;

    private boolean canAccessManifest;

    private boolean canAccessStops;
}