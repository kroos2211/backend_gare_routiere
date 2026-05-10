package com.mahta.backend_gare_routiere.entity;

import com.mahta.backend_gare_routiere.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String departureCity;

    private String arrivalCity;

    private LocalDateTime departureTime;

    private LocalDateTime arrivalTime;

    private double price;

    private int availableSeats;

    @Column(nullable = false)
    private int capacity = 50;

    @OneToMany(
            mappedBy = "trip",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @OrderBy("orderIndex ASC")
    private List<Stop> stops;
}