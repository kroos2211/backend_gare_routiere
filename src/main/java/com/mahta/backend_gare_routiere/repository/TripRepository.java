package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByDepartureCityAndArrivalCityAndDepartureTimeBetween(
            String departure,
            String arrival,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<Trip> findFirstByDepartureCityAndArrivalCityAndDepartureTimeAfterAndAvailableSeatsGreaterThan(
            String departureCity,
            String arrivalCity,
            LocalDateTime departureTime,
            int availableSeats
    );

    List<Trip> findByDepartureCityAndDepartureTimeAfter(
            String departureCity,
            LocalDateTime time
    );
}