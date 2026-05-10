package com.mahta.backend_gare_routiere.service;

import com.mahta.backend_gare_routiere.dto.request.AuthRequests;
import com.mahta.backend_gare_routiere.dto.response.AuthResponses;
import com.mahta.backend_gare_routiere.entity.User;
import com.mahta.backend_gare_routiere.enums.UserRole;
import com.mahta.backend_gare_routiere.exception.AppException;
import com.mahta.backend_gare_routiere.repository.UserRepository;
import com.mahta.backend_gare_routiere.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    private static final long ACCESS_TOKEN_TTL_SEC = 86_400L;

    @Transactional(readOnly = true)
    public AuthResponses.AuthResponse login(AuthRequests.LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getEmail(),
                            req.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new AppException(
                    "Invalid email or password",
                    HttpStatus.UNAUTHORIZED
            );
        }

        User user = userRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new AppException(
                        "User not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!user.isActive()) {
            throw new AppException(
                    "Account is deactivated. Contact an administrator.",
                    HttpStatus.FORBIDDEN
            );
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponses.AuthResponse register(AuthRequests.RegisterRequest req) {

        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new AppException(
                    "Email already in use",
                    HttpStatus.CONFLICT
            );
        }

        User user = User.builder()
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .cin(req.getCin())
                .role(UserRole.TRAVELER)
                .preferredLang(req.getPreferredLang())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        log.info("New traveler registered: {}", savedUser.getEmail());

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponses.AuthResponse refreshToken(AuthRequests.RefreshTokenRequest req) {

        String token = req.getRefreshToken();

        if (!jwtTokenProvider.validateToken(token)) {
            throw new AppException(
                    "Invalid or expired refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (!jwtTokenProvider.isRefreshToken(token)) {
            throw new AppException(
                    "Token is not a refresh token",
                    HttpStatus.UNAUTHORIZED
            );
        }

        User user = userRepository.findById(
                        jwtTokenProvider.getUserIdFromToken(token)
                )
                .orElseThrow(() -> new AppException(
                        "User not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!user.isActive()) {
            throw new AppException(
                    "Account is deactivated. Contact an administrator.",
                    HttpStatus.FORBIDDEN
            );
        }

        return buildAuthResponse(user);
    }

    private AuthResponses.AuthResponse buildAuthResponse(User user) {

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return AuthResponses.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(ACCESS_TOKEN_TTL_SEC)
                .user(AuthResponses.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .preferredLang(user.getPreferredLang())
                        .build())
                .build();
    }
}