package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.entity.PromoCode;
import com.mahta.backend_gare_routiere.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PromoService {

    private final PromoCodeRepository promoRepository;

    public double applyPromo(String code, double price) {
        return applyPromoInternal(code, price);
    }

    public double previewPromo(String code, double price) {
        return applyPromoInternal(code, price);
    }

    private double applyPromoInternal(String code, double price) {

        if (code == null || code.isBlank()) {
            return price;
        }

        PromoCode promo = promoRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid promo code"
                ));

        if (!promo.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promo code inactive"
            );
        }

        if (promo.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Promo code expired"
            );
        }

        double finalPrice;

        if (promo.getFixedAmount() != null && promo.getFixedAmount() > 0) {
            finalPrice = Math.max(0, price - promo.getFixedAmount());
        } else {
            finalPrice = price - (price * promo.getDiscountPercentage() / 100.0);
            finalPrice = Math.max(0, finalPrice);
        }

        return Math.round(finalPrice * 100.0) / 100.0;
    }
}