package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.dto.request.TripSearchRequest;
import com.mahta.backend_gare_routiere.dto.response.TripResponse;
import com.mahta.backend_gare_routiere.dto.response.TripTrackingResponse;
import com.mahta.backend_gare_routiere.entity.Stop;
import com.mahta.backend_gare_routiere.entity.Trip;
import com.mahta.backend_gare_routiere.repository.SeatRepository;
import com.mahta.backend_gare_routiere.repository.StopRepository;
import com.mahta.backend_gare_routiere.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final SeatRepository seatRepository;
    private final StopRepository stopRepository;
    private final TripRepository tripRepository;

    public TripTrackingResponse trackTrip(Long tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Trip not found"
                ));

        List<Stop> stops =
                stopRepository.findByTripIdOrderByOrderIndex(tripId);

        LocalDateTime now = LocalDateTime.now();

        /*
         * =========================
         * FALLBACK SANS STOPS
         * =========================
         */
        if (stops.isEmpty()) {

            LocalDateTime departure = trip.getDepartureTime();
            LocalDateTime arrival = trip.getArrivalTime();

            long totalMinutes =
                    Duration.between(departure, arrival).toMinutes();

            long passedMinutes =
                    Duration.between(departure, now).toMinutes();

            int progress;

            if (now.isBefore(departure)) {
                progress = 0;
            } else if (now.isAfter(arrival)) {
                progress = 100;
            } else {
                progress = (int) (
                        (passedMinutes * 100) / totalMinutes
                );
            }

            String status;

            if (progress >= 100) {
                status = "ARRIVED";
            } else if (progress == 0) {
                status = "NOT_STARTED";
            } else {
                status = "ON_ROUTE";
            }

            return new TripTrackingResponse(
                    progress,
                    trip.getDepartureCity(),
                    progress >= 100
                            ? "ARRIVED"
                            : trip.getArrivalCity(),
                    0,
                    status
            );
        }

        /*
         * =========================
         * TRACKING AVEC STOPS
         * =========================
         */

        int total = stops.size();
        int passed = 0;

        Stop current = null;
        Stop next = null;

        for (Stop stop : stops) {

            LocalDateTime time =
                    stop.getActualTime() != null
                            ? stop.getActualTime()
                            : stop.getScheduledTime();

            if (time.isBefore(now)) {
                passed++;
                current = stop;
            } else {
                next = stop;
                break;
            }
        }

        int progress = (int) (((double) passed / total) * 100);
        progress = Math.max(0, Math.min(progress, 100));

        int delay = 0;

        if (current != null && current.getActualTime() != null) {

            delay = (int) Duration.between(
                    current.getScheduledTime(),
                    current.getActualTime()
            ).toMinutes();
        }

        String status;

        if (delay <= 0) {
            status = "ON_TIME";
        } else if (delay < 30) {
            status = "DELAYED";
        } else {
            status = "INCIDENT";
        }

        if (progress >= 100) {

            return new TripTrackingResponse(
                    100,
                    stops.get(stops.size() - 1).getCity(),
                    "ARRIVED",
                    delay,
                    "ARRIVED"
            );
        }

        String currentCity;
        String nextCity;

        if (current == null) {

            currentCity = stops.get(0).getCity();

            nextCity = stops.size() > 1
                    ? stops.get(1).getCity()
                    : stops.get(0).getCity();

        } else if (next != null) {

            currentCity = current.getCity();
            nextCity = next.getCity();

        } else {

            currentCity = stops.get(stops.size() - 1).getCity();
            nextCity = "ARRIVED";
        }

        return new TripTrackingResponse(
                progress,
                currentCity,
                nextCity,
                delay,
                status
        );
    }

    public List<TripResponse> searchTrips(TripSearchRequest req) {

        if (req.getDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Date is required"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start = req.getDate().atStartOfDay();
        LocalDateTime end = req.getDate().atTime(23, 59, 59);

        if (start.isBefore(now)) {
            start = now;
        }

        List<Trip> trips =
                tripRepository.findByDepartureCityAndArrivalCityAndDepartureTimeBetween(
                        req.getDepartureCity(),
                        req.getArrivalCity(),
                        start,
                        end
                );

        applySorting(req, trips);

        if (trips.isEmpty()) {
            return findConnections(
                    req.getDepartureCity(),
                    req.getArrivalCity(),
                    start
            );
        }

        return trips.stream()
                .map(this::mapTripResponse)
                .toList();
    }

    public TripResponse getTripById(Long id) {

        Trip trip = tripRepository.findById(id)
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

        return mapTripResponse(trip);
    }

    private void applySorting(
            TripSearchRequest req,
            List<Trip> trips
    ) {

        if (req.getSortBy() == null) {
            return;
        }

        switch (req.getSortBy()) {

            case "price" ->
                    trips.sort(
                            Comparator.comparing(this::calculateDynamicPrice)
                    );

            case "departureTime" ->
                    trips.sort(Comparator.comparing(Trip::getDepartureTime));

            case "duration" ->
                    trips.sort(
                            Comparator.comparing(
                                    t -> Duration.between(
                                            t.getDepartureTime(),
                                            t.getArrivalTime()
                                    )
                            )
                    );
        }
    }

    private List<TripResponse> findConnections(
            String departure,
            String arrival,
            LocalDateTime startTime
    ) {

        List<Trip> firstLegs =
                tripRepository.findByDepartureCityAndDepartureTimeAfter(
                        departure,
                        startTime
                );

        List<TripResponse> results = new ArrayList<>();

        for (Trip t1 : firstLegs) {

            List<Trip> secondLegs =
                    tripRepository
                            .findByDepartureCityAndArrivalCityAndDepartureTimeBetween(
                                    t1.getArrivalCity(),
                                    arrival,
                                    t1.getArrivalTime().plusMinutes(30),
                                    t1.getArrivalTime().plusHours(6)
                            );

            for (Trip t2 : secondLegs) {

                int t1AvailableSeats = getRealAvailableSeats(t1);
                int t2AvailableSeats = getRealAvailableSeats(t2);

                results.add(
                        TripResponse.builder()
                                .departureCity(departure)
                                .arrivalCity(arrival)
                                .departureTime(t1.getDepartureTime())
                                .arrivalTime(t2.getArrivalTime())
                                .price(
                                        calculateDynamicPrice(t1)
                                                + calculateDynamicPrice(t2)
                                )
                                .availableSeats(
                                        Math.min(
                                                t1AvailableSeats,
                                                t2AvailableSeats
                                        )
                                )
                                .isFull(
                                        t1AvailableSeats == 0
                                                || t2AvailableSeats == 0
                                )
                                .connections(List.of(
                                        buildConnection(t1),
                                        buildConnection(t2)
                                ))
                                .build()
                );
            }
        }

        return results;
    }

    private Trip findNextAvailableTrip(Trip trip) {

        return tripRepository
                .findFirstByDepartureCityAndArrivalCityAndDepartureTimeAfterAndAvailableSeatsGreaterThan(
                        trip.getDepartureCity(),
                        trip.getArrivalCity(),
                        trip.getDepartureTime(),
                        0
                )
                .orElse(null);
    }

    private TripResponse mapTripResponse(Trip trip) {

        int availableSeats = getRealAvailableSeats(trip);

        Trip nextTrip = null;

        if (availableSeats == 0) {
            nextTrip = findNextAvailableTrip(trip);
        }

        return TripResponse.builder()
                .id(trip.getId())
                .departureCity(trip.getDepartureCity())
                .arrivalCity(trip.getArrivalCity())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .price(calculateDynamicPrice(trip))
                .availableSeats(availableSeats)
                .isFull(availableSeats == 0)
                .nextAvailableTrip(
                        nextTrip == null
                                ? null
                                : buildConnection(nextTrip)
                )
                .build();
    }

    private TripResponse buildConnection(Trip trip) {

        int availableSeats = getRealAvailableSeats(trip);

        return TripResponse.builder()
                .id(trip.getId())
                .departureCity(trip.getDepartureCity())
                .arrivalCity(trip.getArrivalCity())
                .departureTime(trip.getDepartureTime())
                .arrivalTime(trip.getArrivalTime())
                .price(calculateDynamicPrice(trip))
                .availableSeats(availableSeats)
                .isFull(availableSeats == 0)
                .build();
    }

    private double calculateDynamicPrice(Trip trip) {

        double price = trip.getPrice();

        price = applyTimeFactor(trip, price);
        price = applyDemandFactor(trip, price);

        return Math.round(price * 100.0) / 100.0;
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

        int availableSeats = getRealAvailableSeats(trip);

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

    private int getRealAvailableSeats(Trip trip) {
        return seatRepository
                .findByTripIdAndAvailableTrue(trip.getId())
                .size();
    }
}