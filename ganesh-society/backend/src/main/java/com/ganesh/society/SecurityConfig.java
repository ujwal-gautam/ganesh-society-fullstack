
package com.ganesh.society;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(AdminUserRepository repo) {
        return email -> {
            AdminUser u = repo.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Unknown admin")
                    );

            return User.withUsername(u.email)
                    .password(u.passwordHash)
                    .roles(u.role)
                    .disabled(!u.enabled)
                    .build();
        };
    }

    @Bean
    SecurityFilterChain filter(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        return http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET, "/api/public/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")
                        .anyRequest().denyAll()
                )
                .httpBasic(basic -> {})
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors-origin}") String origin
    ) {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(origin));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
