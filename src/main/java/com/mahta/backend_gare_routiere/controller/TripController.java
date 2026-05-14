package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.request.TripSearchRequest;
import com.mahta.backend_gare_routiere.dto.response.TripResponse;
import com.mahta.backend_gare_routiere.dto.response.TripTrackingResponse;
import com.mahta.backend_gare_routiere.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping("/search")
    public List<TripResponse> searchTrips(@Valid @RequestBody TripSearchRequest request) {
        return tripService.searchTrips(request);
    }

    @GetMapping("/{id}/tracking")
    public TripTrackingResponse track(@PathVariable Long id) {
        return tripService.trackTrip(id);
    }

    @GetMapping("/{id}")
    public TripResponse getTripById(
            @PathVariable Long id,
            @RequestParam(required = false) String boardingCity,
            @RequestParam(required = false) String dropoffCity
    ) {
        return tripService.getTripById(id, boardingCity, dropoffCity);
    }
}