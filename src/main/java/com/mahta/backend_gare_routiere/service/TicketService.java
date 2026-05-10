package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.dto.response.QrVerificationResponse;
import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final BookingRepository bookingRepository;
    private final PdfService pdfService;
    private final QrSecurityService qrSecurityService;

    public byte[] generateTicketPdf(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        return pdfService.generateTicket(booking);
    }

    public QrVerificationResponse verifyTicket(String qrContent) {

        try {
            if (!qrContent.contains("|SIGN:")) {
                return new QrVerificationResponse(false, "INVALID FORMAT ❌");
            }

            String[] parts = qrContent.split("\\|");

            if (parts.length != 2) {
                return new QrVerificationResponse(false, "INVALID FORMAT ❌");
            }

            String data = parts[0];
            String signature = parts[1].replace("SIGN:", "");

            if (!qrSecurityService.isValid(data, signature)) {
                return new QrVerificationResponse(false, "FAKE QR ❌");
            }

            if (!data.startsWith("BOOKING_ID:")) {
                return new QrVerificationResponse(false, "INVALID QR DATA ❌");
            }

            Long bookingId = Long.parseLong(data.replace("BOOKING_ID:", ""));

            Booking booking = bookingRepository.findById(bookingId)
                    .orElse(null);

            if (booking == null) {
                return new QrVerificationResponse(false, "BOOKING NOT FOUND ❌");
            }

            if (booking.isUsed()) {
                return new QrVerificationResponse(false, "ALREADY USED ❌");
            }

            if (booking.getStatus() != BookingStatus.PAID) {
                return new QrVerificationResponse(false, "NOT PAID ❌");
            }

            booking.setUsed(true);
            bookingRepository.save(booking);

            return new QrVerificationResponse(true, "VALID TICKET ✅");

        } catch (Exception e) {
            return new QrVerificationResponse(false, "INVALID QR ❌");
        }
    }
}