package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.response.SeatResponse;
import com.mahta.backend_gare_routiere.entity.Seat;
import com.mahta.backend_gare_routiere.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatRepository seatRepository;

    @GetMapping("/trip/{tripId}")
    public List<SeatResponse> getSeatsByTrip(@PathVariable Long tripId) {

        List<Seat> seats = seatRepository.findAllSeatsByTripOrdered(tripId);

        return seats.stream()
                .map(seat -> new SeatResponse(
                        seat.getId(),
                        seat.getSeatNumber(),
                        seat.isAvailable(),
                        seat.getType()
                ))
                .toList();
    }
}