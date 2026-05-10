package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.entity.Booking;
import com.mahta.backend_gare_routiere.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderScheduler {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void run() {

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(24);

        List<Booking> bookings =
                bookingRepository.findUpcomingBookings(now, end);

        for (Booking booking : bookings) {

            LocalDateTime departure = booking.getTrip().getDepartureTime();

            long minutes = Duration.between(now, departure).toMinutes();

            if (minutes < 0) {
                continue;
            }

            handle30min(booking, minutes);
            handle2h(booking, minutes);
            handle24h(booking, minutes);
        }
    }

    private void handle30min(Booking booking, long minutes) {

        if (minutes <= 30 && minutes > 28) {

            if (Boolean.TRUE.equals(booking.getReminder30minSent())) {
                log.debug("30min reminder already sent for booking {}", booking.getId());
                return;
            }

            notificationService.send(
                    booking,
                    "Rappel 30 min : Préparez votre QR code."
            );

            booking.setReminder30minSent(true);
            bookingRepository.save(booking);

            log.info("30min reminder sent for booking {}", booking.getId());
        }
    }

    private void handle2h(Booking booking, long minutes) {

        if (minutes <= 120 && minutes > 118) {

            if (Boolean.TRUE.equals(booking.getReminder2hSent())) {
                log.debug("2h reminder already sent for booking {}", booking.getId());
                return;
            }

            notificationService.send(
                    booking,
                    "Rappel 2h : Votre départ approche."
            );

            booking.setReminder2hSent(true);
            bookingRepository.save(booking);

            log.info("2h reminder sent for booking {}", booking.getId());
        }
    }

    private void handle24h(Booking booking, long minutes) {

        if (minutes <= 1440 && minutes > 1438) {

            if (Boolean.TRUE.equals(booking.getReminder24hSent())) {
                log.debug("24h reminder already sent for booking {}", booking.getId());
                return;
            }

            notificationService.send(
                    booking,
                    "Rappel 24h : "
                            + booking.getTrip().getDepartureCity()
                            + " → "
                            + booking.getTrip().getArrivalCity()
            );

            booking.setReminder24hSent(true);
            bookingRepository.save(booking);

            log.info("24h reminder sent for booking {}", booking.getId());
        }
    }
}