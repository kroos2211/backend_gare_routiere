package com.mahta.backend_gare_routiere.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerManifestResponse {

    private Long bookingId;

    private String passengerName;

    private String seatNumber;

    private String ticketQrCode;

    private boolean boarded;
}