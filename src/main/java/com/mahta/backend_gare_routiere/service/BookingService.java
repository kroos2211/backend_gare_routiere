package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.dto.request.BookingRequest;
import com.mahta.backend_gare_routiere.entity.*;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.enums.SeatType;
import com.mahta.backend_gare_routiere.enums.TariffCategory;
import com.mahta.backend_gare_routiere.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final PromoService promoService;
    private final SeatRepository seatRepository;
    private final StopRepository stopRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final PromoCodeRepository promoRepository;

    @Transactional
    public Booking createBooking(BookingRequest req, User user) {

        Trip trip = tripRepository.findById(req.getTripId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip already departed"
            );
        }

        validateRouteSegment(
                trip,
                req.getBoardingCity(),
                req.getDropoffCity()
        );

        Seat seat = seatRepository
                .findByTripIdAndSeatNumber(trip.getId(), req.getSeatNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat not found"
                ));

        if (!seat.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seat already booked"
            );
        }

        SeatType requestedSeatType = resolveSeatType(req.getSeatType());

        if (!seat.getType().equals(requestedSeatType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seat type mismatch"
            );
        }

        TariffCategory category = resolveTariffCategory(req.getTariffCategory());

        double price = calculatePrice(
                trip,
                req.getBoardingCity(),
                req.getDropoffCity(),
                category,
                req.isBaby(),
                false
        );

        if (req.isHasBagage()) {
            price += 20;
        }

        price = promoService.applyPromo(req.getPromoCode(), price);
        price = roundPrice(price);

        Booking booking = Booking.builder()
                .trip(trip)
                .user(user)
                .tariffCategory(category)
                .totalPrice(price)
                .status(BookingStatus.PENDING)
                .prioritySeat(req.isPrioritySeat())
                .baby(req.isBaby())
                .pendingExtraAmount(0.0)
                .creditAmount(0.0)
                .boardingCity(req.getBoardingCity())
                .dropoffCity(req.getDropoffCity())
                .paidAmount(price)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        Ticket ticket = Ticket.builder()
                .booking(savedBooking)
                .seatNumber(req.getSeatNumber())
                .category(req.getTariffCategory())
                .qrCode("QR-" + savedBooking.getId())
                .build();

        ticketRepository.save(ticket);

        seat.setAvailable(false);
        seatRepository.save(seat);

        return savedBooking;
    }

    public double previewPrice(BookingRequest req) {

        Trip trip = tripRepository.findById(req.getTripId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip already departed"
            );
        }

        validateRouteSegment(
                trip,
                req.getBoardingCity(),
                req.getDropoffCity()
        );

        Seat seat = seatRepository
                .findByTripIdAndSeatNumber(trip.getId(), req.getSeatNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat not found"
                ));

        if (!seat.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seat already booked"
            );
        }

        TariffCategory category = resolveTariffCategory(req.getTariffCategory());

        double price = calculatePrice(
                trip,
                req.getBoardingCity(),
                req.getDropoffCity(),
                category,
                req.isBaby(),
                false
        );

        if (req.isHasBagage()) {
            price += 20;
        }

        return roundPrice(
                promoService.previewPromo(req.getPromoCode(), price)
        );
    }

    @Transactional
    public double cancelBooking(Long bookingId, User user) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized"
            );
        }

        if (booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible d'annuler après le début du voyage"
            );
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Already cancelled"
            );
        }

        long hours = Duration.between(
                LocalDateTime.now(),
                booking.getTrip().getDepartureTime()
        ).toHours();

        double refund;

        if (hours >= 48) {
            refund = booking.getTotalPrice();
        } else if (hours >= 24) {
            refund = booking.getTotalPrice() * 0.5;
        } else {
            refund = 0;
        }

        Ticket ticket = ticketRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        Seat seat = seatRepository
                .findByTripIdAndSeatNumber(
                        booking.getTrip().getId(),
                        ticket.getSeatNumber()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat not found"
                ));

        seat.setAvailable(true);
        seatRepository.save(seat);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCanceledAt(LocalDateTime.now());

        bookingRepository.save(booking);

        return roundPrice(refund);
    }

    @Transactional
    public Booking modifyBooking(
            Long bookingId,
            Long newTripId,
            String newSeatNumber,
            String newBoardingCity,
            String newDropoffCity,
            User user
    ) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized"
            );
        }

        if (booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible de modifier après le début du voyage"
            );
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking cancelled"
            );
        }

        if (booking.getStatus() == BookingStatus.PENDING_EXTRA_PAYMENT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please pay the pending extra amount before another modification"
            );
        }

        Trip oldTrip = booking.getTrip();

        Trip newTrip = tripRepository.findById(newTripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        if (newTrip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trip already departed"
            );
        }

        validateRouteSegment(
                newTrip,
                newBoardingCity,
                newDropoffCity
        );

        Ticket ticket = ticketRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        Seat oldSeat = seatRepository
                .findByTripIdAndSeatNumber(
                        oldTrip.getId(),
                        ticket.getSeatNumber()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Old seat not found"
                ));

        Seat newSeat = seatRepository
                .findByTripIdAndSeatNumber(
                        newTrip.getId(),
                        newSeatNumber
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "New seat not found"
                ));

        boolean sameSeat =
                oldTrip.getId().equals(newTrip.getId())
                        && oldSeat.getSeatNumber().equals(newSeatNumber);

        if (!sameSeat && !newSeat.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seat not available"
            );
        }

        double oldPrice = booking.getPaidAmount();

        boolean modificationFee = booking.isModificationUsed();

        double newPrice = calculatePrice(
                newTrip,
                newBoardingCity,
                newDropoffCity,
                booking.getTariffCategory(),
                booking.isBaby(),
                modificationFee
        );

        newPrice = roundPrice(newPrice);

        double difference = roundPrice(newPrice - oldPrice);



        if (!booking.isModificationUsed()) {
            booking.setModificationUsed(true);
        }
        if (difference > 0) {

            booking.setPendingTrip(newTrip);

            booking.setPendingSeatNumber(newSeatNumber);

            booking.setPendingBoardingCity(newBoardingCity);

            booking.setPendingDropoffCity(newDropoffCity);

            booking.setPendingTotalPrice(newPrice);

        } else {

            if (!sameSeat) {

                oldSeat.setAvailable(true);
                seatRepository.save(oldSeat);

                newSeat.setAvailable(false);
                seatRepository.save(newSeat);
            }

            ticket.setSeatNumber(newSeatNumber);
            ticketRepository.save(ticket);

            booking.setTrip(newTrip);

            booking.setBoardingCity(newBoardingCity);

            booking.setDropoffCity(newDropoffCity);

            booking.setTotalPrice(newPrice);

            booking.setPaidAmount(newPrice);
        }



        if (difference > 0) {
            handleMoreExpensiveModification(booking, difference);
        } else if (difference < 0) {
            handleCheaperModification(booking, Math.abs(difference));
        } else {
            handleSamePriceModification(booking);
        }

        Booking updatedBooking = bookingRepository.save(booking);

        if (updatedBooking.getStatus() == BookingStatus.PAID) {
            sendModifiedTicketEmailSafely(updatedBooking);
        }

        return updatedBooking;
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(User user) {
        return bookingRepository.findByUserId(user.getId());
    }

    private void handleMoreExpensiveModification(

            Booking booking,
            double difference
    ) {
        booking.setStatus(
                BookingStatus.PENDING_EXTRA_PAYMENT
        );
        double roundedDifference = roundPrice(difference);

        if (roundedDifference <= 0) {
            handleSamePriceModification(booking);
            return;
        }

        booking.setPendingExtraAmount(roundedDifference);
        booking.setCreditAmount(0.0);
        booking.setCreditPromoCode(null);
        booking.setStatus(BookingStatus.PENDING_EXTRA_PAYMENT);
    }

    private void handleCheaperModification(
            Booking booking,
            double creditAmount
    ) {

        booking.setPendingExtraAmount(0.0);
        booking.setStatus(BookingStatus.PAID);

        String promoCodeValue =
                "MA7TA-" +
                        UUID.randomUUID()
                                .toString()
                                .substring(0, 8)
                                .toUpperCase();

        PromoCode promo = PromoCode.builder()
                .code(promoCodeValue)
                .discountPercentage(0)
                .fixedAmount(roundPrice(creditAmount))
                .expirationDate(LocalDateTime.now().plusMonths(6))
                .active(true)
                .build();

        promoRepository.save(promo);

        booking.setCreditAmount(roundPrice(creditAmount));
        booking.setCreditPromoCode(promoCodeValue);

        sendCreditEmailSafely(
                booking,
                promoCodeValue,
                roundPrice(creditAmount)
        );
    }

    private void handleSamePriceModification(Booking booking) {

        booking.setPendingExtraAmount(0.0);
        booking.setCreditAmount(0.0);
        booking.setCreditPromoCode(null);
        booking.setStatus(BookingStatus.PAID);
    }

    private double calculatePrice(
            Trip trip,
            String departureCity,
            String arrivalCity,
            TariffCategory category,
            boolean baby,
            boolean modificationFee
    ) {

        double price = calculateBaseSegmentPrice(
                trip,
                departureCity,
                arrivalCity
        );

        price = applyTimeFactor(trip, price);
        price = applyDemandFactor(trip, price);
        price = applyTariff(price, category);

        if (baby) {
            price = price * 0.7;
        }

        if (modificationFee) {
            price += 20;
        }

        return roundPrice(price);
    }

    private double calculateBaseSegmentPrice(
            Trip trip,
            String departure,
            String arrival
    ) {

        List<Stop> stops =
                stopRepository.findByTripIdOrderByOrderIndex(trip.getId());

        List<String> cities = new ArrayList<>();

        cities.add(trip.getDepartureCity());

        for (Stop stop : stops) {
            cities.add(stop.getCity());
        }

        cities.add(trip.getArrivalCity());

        int departureIndex = cities.indexOf(departure);
        int arrivalIndex = cities.indexOf(arrival);

        if (departureIndex == -1 || arrivalIndex == -1 || departureIndex >= arrivalIndex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid route segment"
            );
        }

        double total = 0;

        for (int i = departureIndex + 1; i <= arrivalIndex; i++) {

            String city = cities.get(i);

            Stop stop = stops.stream()
                    .filter(s -> s.getCity().equals(city))
                    .findFirst()
                    .orElse(null);

            if (stop != null) {
                total += stop.getSegmentPrice();
            } else {
                double remaining =
                        trip.getPrice()
                                - stops.stream()
                                .mapToDouble(Stop::getSegmentPrice)
                                .sum();

                total += remaining;
            }
        }

        return roundPrice(total);
    }

    private void validateRouteSegment(
            Trip trip,
            String departure,
            String arrival
    ) {

        calculateBaseSegmentPrice(
                trip,
                departure,
                arrival
        );
    }

    private double applyTariff(double price, TariffCategory category) {

        return switch (category) {
            case STUDENT -> price * 0.8;
            case CHILD -> price * 0.7;
            case MILITARY -> price * 0.85;
            case SENIOR -> price * 0.75;
            default -> price;
        };
    }

    private double applyTimeFactor(Trip trip, double price) {

        long hoursBefore = Duration
                .between(LocalDateTime.now(), trip.getDepartureTime())
                .toHours();

        if (hoursBefore >= 24 * 7) {
            return price * 0.9;
        }

        if (hoursBefore < 24) {
            return price * 1.15;
        }

        return price;
    }

    private double applyDemandFactor(Trip trip, double price) {

        int availableSeats = seatRepository
                .findByTripIdAndAvailableTrue(trip.getId())
                .size();

        int takenSeats = trip.getCapacity() - availableSeats;

        double rate = (double) takenSeats / trip.getCapacity();

        if (rate < 0.5) {
            return price;
        }

        if (rate < 0.8) {
            return price * 1.10;
        }

        return price * 1.25;
    }

    private SeatType resolveSeatType(String seatType) {

        try {
            return SeatType.valueOf(seatType.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid seat type"
            );
        }
    }

    private TariffCategory resolveTariffCategory(String tariffCategory) {

        try {
            return TariffCategory.valueOf(tariffCategory.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid tariff category"
            );
        }
    }

    private double roundPrice(double price) {
        return Math.round(price * 100.0) / 100.0;
    }

    private void sendModifiedTicketEmailSafely(Booking booking) {

        try {
            byte[] pdf = pdfService.generateTicket(booking);

            emailService.sendEmailWithAttachment(
                    booking.getUser().getEmail(),
                    "Votre ticket Ma7ta.ma a été modifié",
                    "Bonjour,\n\n" +
                            "Votre réservation #" + booking.getId() +
                            " a été modifiée avec succès.\n" +
                            "Votre nouveau ticket PDF est en pièce jointe.\n\n" +
                            "Trajet : " +
                            booking.getBoardingCity() +
                            " → " +
                            booking.getDropoffCity() +
                            "\n\nMerci d'utiliser Ma7ta.ma.",
                    pdf
            );

        } catch (Exception e) {
            System.err.println("MODIFY EMAIL/PDF ERROR: " + e.getMessage());
        }
    }

    private void sendCreditEmailSafely(
            Booking booking,
            String promoCode,
            double creditAmount
    ) {

        try {
            emailService.sendSimpleEmail(
                    booking.getUser().getEmail(),
                    "Avoir Ma7ta.ma généré",
                    "Bonjour,\n\n" +
                            "Suite à la modification de votre réservation #" +
                            booking.getId() +
                            ", un avoir de " +
                            String.format("%.2f", creditAmount) +
                            " MAD a été généré.\n\n" +
                            "Code promo : " +
                            promoCode +
                            "\n\nValable 6 mois.\n\n" +
                            "Merci d'utiliser Ma7ta.ma."
            );

        } catch (Exception e) {
            System.err.println("CREDIT EMAIL ERROR: " + e.getMessage());
        }
    }
}