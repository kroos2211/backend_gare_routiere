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

    /*
     * =========================
     * TRIP ACTUEL VALIDÉ
     * =========================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    private Trip trip;

    /*
     * =========================
     * PRIX
     * =========================
     */

    @Column(nullable = false)
    private Double totalPrice;

    /*
     * montant réellement payé
     * utilisé pour les futures modifications
     */
    @Column(name = "paid_amount")
    private Double paidAmount = 0.0;

    /*
     * supplément en attente
     */
    @Column(name = "pending_extra_amount")
    private Double pendingExtraAmount = 0.0;

    /*
     * avoir généré
     */
    @Column(name = "credit_amount")
    private Double creditAmount = 0.0;

    @Column(name = "credit_promo_code")
    private String creditPromoCode;

    /*
     * =========================
     * STATUS
     * =========================
     */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private boolean used = false;

    /*
     * =========================
     * OPTIONS
     * =========================
     */

    @Enumerated(EnumType.STRING)
    private TariffCategory tariffCategory;

    @Column(name = "priority_seat")
    private boolean prioritySeat;

    @Column(name = "baby")
    private boolean baby;

    /*
     * =========================
     * MODIFICATION
     * =========================
     */

    @Column(name = "modification_used")
    private boolean modificationUsed = false;

    @Column(name = "modification_count")
    private Integer modificationCount = 0;

    /*
     * =========================
     * SEGMENT RÉSERVÉ
     * =========================
     */

    @Column(name = "boarding_city")
    private String boardingCity;

    @Column(name = "dropoff_city")
    private String dropoffCity;

    /*
     * =========================
     * MODIFICATION EN ATTENTE
     * =========================
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_trip_id")
    private Trip pendingTrip;

    @Column(name = "pending_boarding_city")
    private String pendingBoardingCity;

    @Column(name = "pending_dropoff_city")
    private String pendingDropoffCity;

    @Column(name = "pending_total_price")
    private Double pendingTotalPrice;

    /*
     * =========================
     * ANNULATION
     * =========================
     */

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    /*
     * =========================
     * RAPPELS
     * =========================
     */

    @Column(name = "reminder_24h_sent")
    private Boolean reminder24hSent = false;

    @Column(name = "reminder_2h_sent")
    private Boolean reminder2hSent = false;

    @Column(name = "reminder_30min_sent")
    private Boolean reminder30minSent = false;


    @Column(name = "pending_seat_number")
    private String pendingSeatNumber;

}