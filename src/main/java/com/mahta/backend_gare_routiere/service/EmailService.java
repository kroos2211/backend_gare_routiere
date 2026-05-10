package com.mahta.backend_gare_routiere.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendSimpleEmail(
            String to,
            String subject,
            String content
    ) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);

        mailSender.send(message);

        log.info("Simple email sent to {}", to);
    }

    public void sendEmailWithAttachment(
            String to,
            String subject,
            String content,
            byte[] pdf
    ) {

        try {

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content);

            helper.addAttachment(
                    "ticket.pdf",
                    new ByteArrayResource(pdf)
            );

            mailSender.send(message);

            log.info("Ticket email sent to {}", to);

        } catch (Exception e) {

            log.error("Email sending failed: {}", e.getMessage());

            throw new RuntimeException(
                    "Failed to send email",
                    e
            );
        }
    }
}