package com.ganesh.society;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Configuration
class JwtConfiguration {
    @Bean
    SecretKey jwtSecretKey(@Value("${app.jwt-secret}") String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length < 32)
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 random bytes (Base64 encoded)");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt-issuer}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final long minutes;

    JwtService(JwtEncoder encoder, @Value("${app.jwt-issuer}") String issuer, @Value("${app.jwt-expiration-minutes:15}") long minutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.minutes = minutes;
    }

    // Hash-based security stamp invalidates JWTs after password or role/permission changes.
    String stamp(AdminUser user) {
        try {
            List<String> permissions = new ArrayList<>();
            for (Role role : user.roles) {
                permissions.add("ROLE:" + role.name + ":" + role.active);
                for (Permission p : role.permissions) permissions.add(role.name + ":" + p.code);
            }
            Collections.sort(permissions);
            String data = user.passwordHash + "|" + user.enabled + "|" + user.tokenVersion + "|" + String.join("|", permissions);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    String issue(AdminUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(issuer).subject(user.email).issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(minutes))).id(UUID.randomUUID().toString())
                .claim("token_type", "access").claim("uid", user.id).claim("token_version", user.tokenVersion).claim("stamp", stamp(user)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    long expiresIn() {
        return minutes * 60;
    }
}
