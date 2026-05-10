package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.request.BookingRequest;
import com.mahta.backend_gare_routiere.dto.response.BookingResponse;
import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.entity.User;
import com.mahta.backend_gare_routiere.repository.UserRepository;
import com.mahta.backend_gare_routiere.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.mahta.backend_gare_routiere.dto.response.PricePreviewResponse;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

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
            @RequestParam String seatNumber
    ) {

        User user = getAuthenticatedUser();

        Booking booking = bookingService.modifyBooking(id, newTripId, seatNumber, user);

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

    private BookingResponse mapBookingResponse(Booking booking, User user) {

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(user.getId())
                .tripId(booking.getTrip().getId())
                .departureCity(booking.getTrip().getDepartureCity())
                .arrivalCity(booking.getTrip().getArrivalCity())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .pendingExtraAmount(booking.getPendingExtraAmount())
                .creditAmount(booking.getCreditAmount())
                .creditPromoCode(booking.getCreditPromoCode())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}