package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.trip t
        JOIN FETCH b.user u
        WHERE u.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findByUserId(@Param("userId") UUID userId);

    List<Booking> findByTripId(Long tripId);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.trip t
        JOIN FETCH b.user u
        WHERE b.status = 'PAID'
        AND t.departureTime BETWEEN :start AND :end
    """)
    List<Booking> findUpcomingBookings(
            LocalDateTime start,
            LocalDateTime end
    );
}