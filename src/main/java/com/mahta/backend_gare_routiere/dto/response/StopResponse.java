package com.mahta.backend_gare_routiere.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopResponse {

    private Long id;

    private String city;

    private int orderIndex;

    private LocalDateTime scheduledTime;

    private LocalDateTime actualTime;

    private boolean validated;
}