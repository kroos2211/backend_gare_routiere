package com.mahta.backend_gare_routiere.dto.response;

import com.mahta.backend_gare_routiere.enums.SeatType;

public record SeatResponse(
        Long id,
        String seatNumber,
        boolean available,
        SeatType type
) {
}