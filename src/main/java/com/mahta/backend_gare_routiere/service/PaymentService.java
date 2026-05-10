package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.entity.Payment;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.enums.PaymentMethod;
import com.mahta.backend_gare_routiere.enums.PaymentStatus;
import com.mahta.backend_gare_routiere.repository.BookingRepository;
import com.mahta.backend_gare_routiere.repository.PaymentRepository;
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
            booking.setPendingExtraAmount(0.0);
        }

        booking.setStatus(BookingStatus.PAID);

        Payment savedPayment = paymentRepository.save(payment);
        bookingRepository.save(booking);

        sendTicketEmailSafely(booking, isExtraPayment);

        return savedPayment;
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