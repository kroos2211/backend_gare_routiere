package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.request.BookingRequest;
import com.mahta.backend_gare_routiere.dto.response.BookingResponse;
import com.mahta.backend_gare_routiere.dto.response.PricePreviewResponse;
import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.entity.Stop;
import com.mahta.backend_gare_routiere.entity.Trip;
import com.mahta.backend_gare_routiere.entity.User;
import com.mahta.backend_gare_routiere.repository.StopRepository;
import com.mahta.backend_gare_routiere.repository.UserRepository;
import com.mahta.backend_gare_routiere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final StopRepository stopRepository;

    @PostMapping
    public BookingResponse create(@RequestBody BookingRequest req) {
        User user = getAuthenticatedUser();
        Booking booking = bookingService.createBooking(req, user);
        return mapBookingResponse(booking, user);
    }

    @GetMapping("/my")
    public List<BookingResponse> myBookings() {
        User user = getAuthenticatedUser();

        return bookingService.getUserBookings(user)
                .stream()
                .map(booking -> mapBookingResponse(booking, user))
                .toList();
    }

    @PostMapping("/{id}/modify")
    public BookingResponse modify(
            @PathVariable Long id,
            @RequestParam Long newTripId,
            @RequestParam String seatNumber,
            @RequestParam String newBoardingCity,
            @RequestParam String newDropoffCity
    ) {
        User user = getAuthenticatedUser();

        Booking booking = bookingService.modifyBooking(
                id,
                newTripId,
                seatNumber,
                newBoardingCity,
                newDropoffCity,
                user
        );

        return mapBookingResponse(booking, user);
    }

    @PostMapping("/preview-price")
    public PricePreviewResponse previewPrice(@RequestBody BookingRequest req) {
        double totalPrice = bookingService.previewPrice(req);

        return PricePreviewResponse.builder()
                .totalPrice(totalPrice)
                .build();
    }

    @PostMapping("/{id}/cancel")
    public double cancel(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        return bookingService.cancelBooking(id, user);
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String value = authentication.getName();

        if (value.contains("@")) {
            return userRepository.findByEmailIgnoreCase(value)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        return userRepository.findById(UUID.fromString(value))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private BookingResponse mapBookingResponse(
            Booking booking,
            User user
    ) {

        Trip trip = booking.getTrip();

        return BookingResponse.builder()

                /*
                 * =========================
                 * IDS
                 * =========================
                 */

                .id(booking.getId())

                .userId(user.getId())

                .tripId(
                        trip != null
                                ? trip.getId()
                                : null
                )

                /*
                 * =========================
                 * TRAJET GLOBAL
                 * =========================
                 */

                .departureCity(
                        trip != null
                                ? trip.getDepartureCity()
                                : null
                )

                .arrivalCity(
                        trip != null
                                ? trip.getArrivalCity()
                                : null
                )

                .departureTime(
                        trip != null
                                ? trip.getDepartureTime()
                                : null
                )

                /*
                 * =========================
                 * SEGMENT RÉSERVÉ
                 * =========================
                 */

                .boardingCity(
                        booking.getBoardingCity()
                )

                .dropoffCity(
                        booking.getDropoffCity()
                )

                .segmentDepartureTime(
                        trip != null
                                ? resolveSegmentDepartureTime(
                                trip,
                                booking.getBoardingCity()
                        )
                                : null
                )

                .segmentArrivalTime(
                        trip != null
                                ? resolveSegmentArrivalTime(
                                trip,
                                booking.getDropoffCity()
                        )
                                : null
                )

                /*
                 * =========================
                 * PRIX
                 * =========================
                 */

                .totalPrice(
                        booking.getTotalPrice()
                )

                .paidAmount(
                        booking.getPaidAmount()
                )

                .pendingExtraAmount(
                        booking.getPendingExtraAmount()
                )

                .creditAmount(
                        booking.getCreditAmount()
                )

                .creditPromoCode(
                        booking.getCreditPromoCode()
                )

                /*
                 * =========================
                 * STATUS
                 * =========================
                 */

                .status(
                        booking.getStatus().name()
                )

                /*
                 * =========================
                 * MODIFICATION
                 * =========================
                 */

                .modificationUsed(
                        booking.isModificationUsed()
                )

                .modificationCount(
                        booking.getModificationCount()
                )

                /*
                 * =========================
                 * PENDING MODIFICATION
                 * =========================
                 */

                .pendingTripId(
                        booking.getPendingTrip() != null
                                ? booking.getPendingTrip().getId()
                                : null
                )

                .pendingBoardingCity(
                        booking.getPendingBoardingCity()
                )

                .pendingDropoffCity(
                        booking.getPendingDropoffCity()
                )

                .pendingTotalPrice(
                        booking.getPendingTotalPrice()
                )

                /*
                 * =========================
                 * DATES
                 * =========================
                 */

                .createdAt(
                        booking.getCreatedAt()
                )

                .build();
    }

    private LocalDateTime resolveSegmentDepartureTime(Trip trip, String city) {
        if (city == null || city.equals(trip.getDepartureCity())) {
            return trip.getDepartureTime();
        }

        return stopRepository.findByTripIdOrderByOrderIndex(trip.getId())
                .stream()
                .filter(stop -> stop.getCity().equals(city))
                .findFirst()
                .map(Stop::getScheduledTime)
                .orElse(trip.getDepartureTime());
    }

    private LocalDateTime resolveSegmentArrivalTime(Trip trip, String city) {
        if (city == null || city.equals(trip.getArrivalCity())) {
            return trip.getArrivalTime();
        }

        return stopRepository.findByTripIdOrderByOrderIndex(trip.getId())
                .stream()
                .filter(stop -> stop.getCity().equals(city))
                .findFirst()
                .map(Stop::getScheduledTime)
                .orElse(trip.getArrivalTime());
    }
}