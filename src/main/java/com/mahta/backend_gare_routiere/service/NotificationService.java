package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.entity.Booking;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void send(Booking booking, String message) {

        try {
            String to = booking.getUser().getEmail();

            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject("Rappel de voyage - Ma7ta.ma");

            helper.setText(
                    """
                    Bonjour,

                    %s

                    Trajet : %s → %s
                    Départ : %s
                    Réservation : #%s

                    Merci d'utiliser Ma7ta.ma.
                    """
                            .formatted(
                                    message,
                                    booking.getTrip().getDepartureCity(),
                                    booking.getTrip().getArrivalCity(),
                                    booking.getTrip().getDepartureTime(),
                                    booking.getId()
                            )
            );

            mailSender.send(mimeMessage);

            log.info(
                    "Reminder email sent to {} | Booking {}",
                    to,
                    booking.getId()
            );

        } catch (Exception e) {
            log.error(
                    "Failed to send reminder email for booking {}",
                    booking.getId(),
                    e
            );
        }
    }
}