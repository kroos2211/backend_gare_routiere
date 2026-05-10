package com.mahta.backend_gare_routiere.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripTrackingResponse {

    private int progress;

    private String currentStop;

    private String nextStop;

    private int delayMinutes;

    private String status;
}