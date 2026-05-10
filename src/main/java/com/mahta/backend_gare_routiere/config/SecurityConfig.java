package com.mahta.backend_gare_routiere.config;

import com.mahta.backend_gare_routiere.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity           // allows @PreAuthorize on individual methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── Public endpoints (no token needed) ─────────────────
    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/ws/**"               // WebSocket upgrade
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        .requestMatchers("/api/v1/trips/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/trips/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/seats/**").permitAll()

                        // Admin
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")

                        // Agency manager
                        .requestMatchers("/api/v1/agency/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        .requestMatchers("/api/v1/lines/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        .requestMatchers("/api/v1/schedules/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        .requestMatchers(HttpMethod.POST, "/api/v1/trips/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        .requestMatchers("/api/v1/refunds/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        .requestMatchers("/api/v1/announcements/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER")

                        // Station manager
                        .requestMatchers("/api/v1/station/**")
                        .hasAnyRole("ADMIN", "STATION_MANAGER")

                        .requestMatchers("/api/v1/parking/**")
                        .hasAnyRole("ADMIN", "STATION_MANAGER")

                        .requestMatchers("/api/v1/ocr/**")
                        .hasAnyRole("ADMIN", "STATION_MANAGER")

                        .requestMatchers("/api/v1/quays/**")
                        .hasAnyRole("ADMIN", "STATION_MANAGER")

                        // Driver
                        .requestMatchers("/api/v1/driver/**")
                        .hasAnyRole("ADMIN", "DRIVER")

                        // Traveler
                        .requestMatchers("/api/v1/tickets/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER", "TRAVELER")

                        .requestMatchers("/api/v1/bookings/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER", "TRAVELER")

                        .requestMatchers("/api/v1/traveler/**")
                        .hasRole("TRAVELER")

                        // Predictions
                        .requestMatchers("/api/v1/predictions/**")
                        .hasAnyRole("ADMIN", "AGENCY_MANAGER", "STATION_MANAGER")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);   // cost factor 12 = ~250ms per hash
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}
