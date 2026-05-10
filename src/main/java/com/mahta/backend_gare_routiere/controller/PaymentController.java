package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.dto.request.PaymentRequest;
import com.mahta.backend_gare_routiere.dto.response.PaymentResponse;
import com.mahta.backend_gare_routiere.entity.Payment;
import com.mahta.backend_gare_routiere.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {

        Payment payment = paymentService.processPayment(
                request.getBookingId(),
                request.getMethod()
        );

        return mapPaymentResponse(payment);
    }

    private PaymentResponse mapPaymentResponse(Payment payment) {

        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .method(payment.getMethod().name())
                .status(payment.getStatus().name())
                .bookingId(payment.getBooking().getId())
                .build();
    }
}