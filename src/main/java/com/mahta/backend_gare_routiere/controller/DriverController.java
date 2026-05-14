package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.request.TicketVerifyRequest;
import com.mahta.backend_gare_routiere.dto.response.DriverDashboardResponse;
import com.mahta.backend_gare_routiere.dto.response.PassengerManifestResponse;
import com.mahta.backend_gare_routiere.dto.response.QrVerificationResponse;
import com.mahta.backend_gare_routiere.dto.response.StopResponse;
import com.mahta.backend_gare_routiere.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.mahta.backend_gare_routiere.dto.response.DriverTripHistoryResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/dashboard")
    public DriverDashboardResponse dashboard(Authentication authentication) {
        return driverService.getDriverDashboard(authentication);
    }
    @GetMapping("/trips/{tripId}/manifest")
    public List<PassengerManifestResponse> manifest(@PathVariable Long tripId) {
        return driverService.getManifest(tripId);
    }
    @PostMapping("/boarding/validate-qr")
    public QrVerificationResponse validateQr(
            @RequestBody @Valid TicketVerifyRequest request
    ) {
        return driverService.validateQr(request);
    }
    @GetMapping("/trips/{tripId}/stops")
    public List<StopResponse> getStops(@PathVariable Long tripId) {
        return driverService.getTripStops(tripId);
    }
    @PutMapping("/stops/{stopId}/validate")
    public StopResponse validateStop(@PathVariable Long stopId) {
        return driverService.validateStop(stopId);
    }
    @GetMapping("/history")
    public List<DriverTripHistoryResponse> history(Authentication authentication) {
        return driverService.getDriverHistory(authentication);
    }
}