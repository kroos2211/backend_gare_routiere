package com.mahta.backend_gare_routiere.controller;

import com.mahta.backend_gare_routiere.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/test-email")
@RequiredArgsConstructor
public class TestEmailController {

    private final EmailService emailService;

    @GetMapping
    public String sendTest() {

        emailService.sendSimpleEmail(
                "TON_EMAIL@gmail.com",
                "Test Spring Boot",
                "Email fonctionne ✅"
        );

        return "Email envoyé";
    }
}