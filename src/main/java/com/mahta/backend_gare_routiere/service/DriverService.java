package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.dto.response.DriverDashboardResponse;
import com.mahta.backend_gare_routiere.dto.response.PassengerManifestResponse;
import com.mahta.backend_gare_routiere.dto.response.QrVerificationResponse;
import com.mahta.backend_gare_routiere.dto.request.TicketVerifyRequest;
import com.mahta.backend_gare_routiere.dto.response.StopResponse;
import com.mahta.backend_gare_routiere.entity.*;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.mahta.backend_gare_routiere.repository.SeatRepository;
import com.mahta.backend_gare_routiere.dto.response.DriverTripHistoryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final QrSecurityService qrSecurityService;
    private final StopRepository stopRepository;

    private boolean isTripFinished(Trip trip) {

        List<Stop> stops =
                stopRepository.findByTripIdOrderByOrderIndex(
                        trip.getId()
                );

        if (stops.isEmpty()) {
            return LocalDateTime.now().isAfter(
                    trip.getArrivalTime()
            );
        }

        boolean allValidated =
                stops.stream().allMatch(Stop::isValidated);

        if (allValidated && LocalDateTime.now().isAfter(trip.getArrivalTime())) {

            trip.setStatus("ARRIVED");
            tripRepository.save(trip);

            List<Booking> bookings = bookingRepository.findByTripId(trip.getId());

            bookings.forEach(booking -> {
                if (booking.getStatus() == BookingStatus.PAID) {
                    booking.setStatus(BookingStatus.COMPLETED);
                }
            });

            bookingRepository.saveAll(bookings);

            return true;
        }

        return false;
    }

    private boolean isScanClosed(Trip trip) {
        return LocalDateTime.now().isAfter(trip.getDepartureTime().plusMinutes(1));
    }

    public List<PassengerManifestResponse> getManifest(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (isTripFinished(trip)) {
            throw new RuntimeException("Ce voyage est terminé. Le manifeste est maintenant en historique.");
        }

        List<Booking> bookings = bookingRepository.findByTripId(tripId);

        return bookings.stream()
                .filter(b -> b.getStatus().name().equals("PAID"))
                .map(booking -> {
                    Ticket ticket = ticketRepository.findByBookingId(booking.getId())
                            .orElseThrow(() -> new RuntimeException("Ticket not found for booking " + booking.getId()));

                    return PassengerManifestResponse.builder()
                            .bookingId(booking.getId())
                            .passengerName(
                                    booking.getUser().getFirstName() + " " + booking.getUser().getLastName()
                            )
                            .seatNumber(ticket.getSeatNumber())
                            .ticketQrCode(ticket.getQrCode())
                            .boarded(booking.isUsed())
                            .build();
                })
                .toList();
    }

    public DriverDashboardResponse getDriverDashboard(Authentication authentication) {

        String email = authentication.getName();

        User driver = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Trip> trips = tripRepository
                .findByDriverIdAndDepartureTimeBetweenOrderByDepartureTimeAsc(
                        driver.getId(),
                        start,
                        end
                );

        Trip trip = trips.stream()
                .filter(t -> !isTripFinished(t))
                .findFirst()
                .orElse(null);

        if (trip == null) {
            return null;
        }

        boolean finished = isTripFinished(trip);

        boolean scanClosed = isScanClosed(trip);

        return DriverDashboardResponse.builder()
                .tripId(trip.getId())
                .departureCity(trip.getDepartureCity())
                .arrivalCity(trip.getArrivalCity())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .status(trip.getStatus())
                .availableSeats(
                        seatRepository
                                .findByTripIdAndAvailableTrue(trip.getId())
                                .size()
                )

                .finished(finished)

                .scanClosed(scanClosed)

                .canScan(!finished && !scanClosed)

                .canAccessManifest(!finished)

                .canAccessStops(!finished)

                .build();
    }

    public QrVerificationResponse validateQr(TicketVerifyRequest request) {

        String qrContent = request.getQrContent();

        if (qrContent == null || !qrContent.startsWith("BOOKING_ID:") || !qrContent.contains("|SIGN:")) {
            return new QrVerificationResponse(false, "Format QR invalide");
        }

        String[] parts = qrContent.split("\\|SIGN:");

        if (parts.length != 2) {
            return new QrVerificationResponse(false, "Format QR invalide");
        }

        String data = parts[0];
        String signature = parts[1];

        boolean validSignature = qrSecurityService.isValid(data, signature);

        if (!validSignature) {
            return new QrVerificationResponse(false, "Signature QR invalide");
        }

        Long bookingId = Long.parseLong(data.replace("BOOKING_ID:", ""));

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        Trip trip = booking.getTrip();

        if (isTripFinished(trip)) {
            return new QrVerificationResponse(false, "Ce voyage est terminé. Scan interdit.");
        }

        if (isScanClosed(trip)) {
            return new QrVerificationResponse(false, "Le scan est fermé après le délai autorisé.");
        }

        if (booking.isUsed()) {
            return new QrVerificationResponse(false, "Ticket déjà utilisé");
        }

        if (!booking.getStatus().name().equals("PAID")) {
            return new QrVerificationResponse(false, "Ticket non payé");
        }

        booking.setUsed(true);
        bookingRepository.save(booking);

        return new QrVerificationResponse(true, "Embarquement validé avec succès");
    }

    public List<StopResponse> getTripStops(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        if (isTripFinished(trip)) {
            throw new RuntimeException("Ce voyage est terminé. Les jalons sont maintenant en historique.");
        }

        List<Stop> stops = stopRepository.findByTripIdOrderByOrderIndex(tripId);

        return stops.stream()
                .map(stop -> StopResponse.builder()
                        .id(stop.getId())
                        .city(stop.getCity())
                        .orderIndex(stop.getOrderIndex())
                        .scheduledTime(stop.getScheduledTime())
                        .actualTime(stop.getActualTime())
                        .validated(stop.isValidated())
                        .build())
                .toList();
    }

    public StopResponse validateStop(Long stopId) {

        Stop stop = stopRepository.findById(stopId)
                .orElseThrow(() -> new RuntimeException("Stop not found"));

        Trip trip = stop.getTrip();

        if (isTripFinished(trip)) {
            throw new RuntimeException("Ce voyage est terminé. Validation des jalons interdite.");
        }

        stop.setValidated(true);
        stop.setActualTime(LocalDateTime.now());

        stopRepository.save(stop);

        return StopResponse.builder()
                .id(stop.getId())
                .city(stop.getCity())
                .orderIndex(stop.getOrderIndex())
                .scheduledTime(stop.getScheduledTime())
                .actualTime(stop.getActualTime())
                .validated(stop.isValidated())
                .build();
    }
    public List<DriverTripHistoryResponse> getDriverHistory(Authentication authentication) {

        String email = authentication.getName();

        User driver = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Driver not found"));

        List<Trip> trips = tripRepository
                .findByDriverIdOrderByDepartureTimeDesc(driver.getId());

        return trips.stream()
                .filter(this::isTripFinished)
                .map(trip -> DriverTripHistoryResponse.builder()
                        .tripId(trip.getId())
                        .departureCity(trip.getDepartureCity())
                        .arrivalCity(trip.getArrivalCity())
                        .departureTime(trip.getDepartureTime())
                        .arrivalTime(trip.getArrivalTime())
                        .status(trip.getStatus())
                        .availableSeats(
                                seatRepository
                                        .findByTripIdAndAvailableTrue(trip.getId())
                                        .size()
                        )
                        .finished(isTripFinished(trip))
                        .build()
                )
                .toList();
    }
}