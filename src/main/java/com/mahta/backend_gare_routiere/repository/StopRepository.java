package com.mahta.backend_gare_routiere.repository;

import com.mahta.backend_gare_routiere.entity.Stop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findByTripIdOrderByOrderIndex(Long tripId);
}