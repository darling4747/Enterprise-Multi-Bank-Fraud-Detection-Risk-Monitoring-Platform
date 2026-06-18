package com.bank.frauddetection.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final ObjectMapper objectMapper;
    private final String secret;
    private final long ttlSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-ttl-seconds:86400}") long ttlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String generateToken(UserDetails userDetails, Long sessionId) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", userDetails.getUsername());
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond());
            payload.put("roles", userDetails.getAuthorities().stream().map(Object::toString).toList());
            if (sessionId != null) {
                payload.put("sid", sessionId);
            }

            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate JWT token", ex);
        }
    }

    public String extractUsername(String token) {
        return claims(token).get("sub").toString();
    }

    public Instant expiresAt(String token) {
        Number exp = (Number) claims(token).get("exp");
        return Instant.ofEpochSecond(exp.longValue());
    }

    public Long extractSessionId(String token) {
        Object sid = claims(token).get("sid");
        if (sid instanceof Number number) {
            return number.longValue();
        }
        if (sid instanceof String value && !value.isBlank()) {
            return Long.parseLong(value);
        }
        return null;
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && expiresAt(token).isAfter(Instant.now()) && signatureValid(token);
    }

    public Instant expiresAtFromNow() {
        return Instant.now().plusSeconds(ttlSeconds);
    }

    private Map<String, Object> claims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Malformed JWT token");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    private boolean signatureValid(String token) {
        String[] parts = token.split("\\.");
        String unsignedToken = parts[0] + "." + parts[1];
        return sign(unsignedToken).equals(parts[2]);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT token", ex);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
