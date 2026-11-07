package com.ganesh.society;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http, @Qualifier("corsConfigurationSource") CorsConfigurationSource cors, JwtDecoder decoder, JwtService jwtService, AdminUserRepository users, TokenService tokens) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            if (!"access".equals(jwt.getClaimAsString("token_type")))
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Wrong token type", null));
            AdminUser user = users.findByEmail(jwt.getSubject()).filter(u -> u.enabled).orElseThrow(() -> new BadCredentialsException("User unavailable"));
            if (!(jwt.getClaim("uid") instanceof Number tokenId) || user.id.longValue() != tokenId.longValue() || !jwtService.stamp(user).equals(jwt.getClaimAsString("stamp")))
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Token revoked by account change", null));
            if (!(jwt.getClaim("token_version") instanceof Number version) || !Objects.equals(version.longValue(), user.tokenVersion))
                throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token", "Token version revoked", null));
            return new ArrayList<GrantedAuthority>(tokens.principal(user).getAuthorities());
        });
        return http.cors(c -> c.configurationSource(cors)).csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(converter)))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/admin/users/**", "/api/admin/roles/**", "/api/admin/permissions/**").hasAuthority("ROLE_SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/members").hasAuthority("MEMBER_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/members/**").hasAuthority("MEMBER_UPDATE")
                        .requestMatchers(HttpMethod.POST, "/api/admin/contributions").hasAuthority("CONTRIBUTION_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/contributions/*").hasAuthority("CONTRIBUTION_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/api/admin/contributions/*").hasAuthority("CONTRIBUTION_DELETE")
                        .requestMatchers(HttpMethod.GET, "/api/admin/members/*/contributions", "/api/admin/members/*/contribution-changes", "/api/admin/contributions/*/history").hasAuthority("CONTRIBUTION_VIEW")
                        .requestMatchers(HttpMethod.POST, "/api/admin/loans").hasAuthority("LOAN_CREATE")
                        .requestMatchers(HttpMethod.POST, "/api/admin/loans/*/repayments").hasAuthority("LOAN_REPAYMENT_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/api/admin/settings").hasAuthority("SETTINGS_UPDATE")
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().denyAll())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origin}") String origins) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        c.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);
        return source;
    }
}
