package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.entity.Payment;
import com.mahta.backend_gare_routiere.entity.Seat;
import com.mahta.backend_gare_routiere.entity.Ticket;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.enums.PaymentMethod;
import com.mahta.backend_gare_routiere.enums.PaymentStatus;
import com.mahta.backend_gare_routiere.repository.BookingRepository;
import com.mahta.backend_gare_routiere.repository.PaymentRepository;
import com.mahta.backend_gare_routiere.repository.SeatRepository;
import com.mahta.backend_gare_routiere.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Transactional
    public Payment processPayment(Long bookingId, String method) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        boolean isExtraPayment =
                booking.getStatus() == BookingStatus.PENDING_EXTRA_PAYMENT;

        if (booking.getStatus() == BookingStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Already paid"
            );
        }

        if (booking.getStatus() != BookingStatus.PENDING && !isExtraPayment) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking is not payable"
            );
        }

        if (!isExtraPayment && paymentRepository.existsByBooking(booking)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment already exists"
            );
        }

        PaymentMethod paymentMethod;

        try {
            paymentMethod = PaymentMethod.valueOf(method.toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment method"
            );
        }

        double amountToPay = isExtraPayment
                ? booking.getPendingExtraAmount()
                : booking.getTotalPrice();

        if (amountToPay <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid payment amount"
            );
        }

        Payment payment = Payment.builder()
                .amount(amountToPay)
                .method(paymentMethod)
                .status(PaymentStatus.SUCCESS)
                .booking(booking)
                .build();

        if (isExtraPayment) {
            applyPendingModificationAfterPayment(booking);
        } else {
            booking.setPaidAmount(booking.getTotalPrice());
        }

        booking.setStatus(BookingStatus.PAID);

        Payment savedPayment = paymentRepository.save(payment);
        bookingRepository.save(booking);

        sendTicketEmailSafely(booking, isExtraPayment);

        return savedPayment;
    }

    private void applyPendingModificationAfterPayment(Booking booking) {

        if (booking.getPendingTrip() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No pending modification found"
            );
        }

        Ticket ticket = ticketRepository.findByBookingId(booking.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        Seat oldSeat = seatRepository
                .findByTripIdAndSeatNumber(
                        booking.getTrip().getId(),
                        ticket.getSeatNumber()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Old seat not found"
                ));

        Seat newSeat = seatRepository
                .findByTripIdAndSeatNumber(
                        booking.getPendingTrip().getId(),
                        booking.getPendingSeatNumber()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "New seat not found"
                ));

        boolean sameSeat =
                booking.getTrip().getId().equals(booking.getPendingTrip().getId())
                        && ticket.getSeatNumber().equals(booking.getPendingSeatNumber());

        if (!sameSeat && !newSeat.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pending seat is no longer available"
            );
        }

        if (!sameSeat) {
            oldSeat.setAvailable(true);
            seatRepository.save(oldSeat);

            newSeat.setAvailable(false);
            seatRepository.save(newSeat);
        }

        ticket.setSeatNumber(booking.getPendingSeatNumber());
        ticketRepository.save(ticket);

        booking.setTrip(booking.getPendingTrip());
        booking.setBoardingCity(booking.getPendingBoardingCity());
        booking.setDropoffCity(booking.getPendingDropoffCity());
        booking.setTotalPrice(booking.getPendingTotalPrice());
        booking.setPaidAmount(booking.getPendingTotalPrice());

        booking.setPendingExtraAmount(0.0);
        booking.setCreditAmount(0.0);
        booking.setCreditPromoCode(null);

        booking.setPendingTrip(null);
        booking.setPendingBoardingCity(null);
        booking.setPendingDropoffCity(null);
        booking.setPendingSeatNumber(null);
        booking.setPendingTotalPrice(null);
    }

    private void sendTicketEmailSafely(
            Booking booking,
            boolean isExtraPayment
    ) {
        try {
            byte[] pdf = pdfService.generateTicket(booking);

            String subject = isExtraPayment
                    ? "Paiement complémentaire confirmé - Ma7ta.ma"
                    : "Votre ticket Ma7ta.ma";

            String content = isExtraPayment
                    ? "Bonjour,\n\nVotre paiement complémentaire a été confirmé.\n"
                    + "Votre ticket mis à jour est en pièce jointe.\n\n"
                    + "Merci d'utiliser Ma7ta.ma."
                    : "Bonjour,\n\nMerci pour votre achat.\n"
                    + "Votre ticket est en pièce jointe.\n\n"
                    + "Merci d'utiliser Ma7ta.ma.";

            emailService.sendEmailWithAttachment(
                    booking.getUser().getEmail(),
                    subject,
                    content,
                    pdf
            );

        } catch (Exception e) {
            System.err.println("PDF/EMAIL ERROR: " + e.getMessage());
        }
    }
}