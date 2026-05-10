package com.mahta.backend_gare_routiere.entity;

import com.mahta.backend_gare_routiere.entity.base.BaseEntity;
import com.mahta.backend_gare_routiere.enums.BookingStatus;
import com.mahta.backend_gare_routiere.enums.TariffCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    @Column(nullable = false)
    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private boolean used = false;

    @Enumerated(EnumType.STRING)
    private TariffCategory tariffCategory;

    @Column(name = "priority_seat")
    private boolean prioritySeat;

    @Column(name = "baby")
    private boolean baby;

    @Column(name = "modification_used")
    private boolean modificationUsed = false;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "reminder_24h_sent")
    private Boolean reminder24hSent = false;

    @Column(name = "reminder_2h_sent")
    private Boolean reminder2hSent = false;

    @Column(name = "reminder_30min_sent")
    private Boolean reminder30minSent = false;
    @Column(name = "pending_extra_amount")
    private Double pendingExtraAmount = 0.0;

    @Column(name = "credit_amount")
    private Double creditAmount = 0.0;

    @Column(name = "credit_promo_code")
    private String creditPromoCode;
}