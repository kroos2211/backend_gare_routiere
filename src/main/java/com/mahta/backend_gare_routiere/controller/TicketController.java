package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.response.QrVerificationResponse;
import com.mahta.backend_gare_routiere.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.mahta.backend_gare_routiere.dto.request.TicketVerifyRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{bookingId}/pdf")
    public ResponseEntity<byte[]> downloadTicket(@PathVariable Long bookingId) {

        byte[] pdf = ticketService.generateTicketPdf(bookingId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/verify")
    public ResponseEntity<QrVerificationResponse> verifyTicket(
            @Valid @RequestBody TicketVerifyRequest request
    ) {
        return ResponseEntity.ok(
                ticketService.verifyTicket(request.getQrContent())
        );
    }
}