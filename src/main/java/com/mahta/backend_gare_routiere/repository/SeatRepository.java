package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTripIdAndAvailableTrue(Long tripId);

    Optional<Seat> findByTripIdAndSeatNumber(
            Long tripId,
            String seatNumber
    );

    @Query(
            value = """
                    SELECT *
                    FROM seat
                    WHERE trip_id = :tripId
                    ORDER BY
                        substring(seat_number from '^[A-Z]+'),
                        CAST(substring(seat_number from '[0-9]+$') AS INTEGER)
                    """,
            nativeQuery = true
    )
    List<Seat> findAllSeatsByTripOrdered(Long tripId);
}