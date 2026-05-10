package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.Payment;
import com.mahta.backend_gare_routiere.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 🔥 AJOUTE ÇA
    boolean existsByBooking(Booking booking);
}