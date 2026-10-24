package br.com.yann.sextafeira.identity.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final String HEADER_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper;
    private final byte[] secretKeyBytes;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtService(
            @Value("${sexta.security.jwt.secret:}") String secret,
            @Value("${sexta.security.jwt.access-expiration-seconds:3600}") long accessTokenExpirationSeconds,
            @Value("${sexta.security.jwt.refresh-expiration-seconds:2592000}") long refreshTokenExpirationSeconds
    ) {
        this.objectMapper = new ObjectMapper();
        String effectiveSecret = (secret != null && !secret.isBlank())
                ? secret
                : "dev-local-secret-key-sexta-feira-2026-minimum-256-bits-secure";
        this.secretKeyBytes = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    public String gerarAccessToken(Long usuarioId, String email) {
        return criarToken(usuarioId, email, "access", accessTokenExpirationSeconds);
    }

    public String gerarRefreshToken(Long usuarioId, String email) {
        return criarToken(usuarioId, email, "refresh", refreshTokenExpirationSeconds);
    }

    public Map<String, Object> validarToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String dataToSign = parts[0] + "." + parts[1];
        String expectedSig = assinar(dataToSign);

        if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSig.getBytes(StandardCharsets.UTF_8))) {
            return null; // Assinatura inválida
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, new TypeReference<>() {});

            if (claims.containsKey("exp")) {
                long exp = ((Number) claims.get("exp")).longValue();
                if (Instant.now().getEpochSecond() > exp) {
                    return null; // Token expirado
                }
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public Long extrairUserId(String token) {
        Map<String, Object> claims = validarToken(token);
        if (claims != null && claims.containsKey("sub")) {
            return Long.valueOf(claims.get("sub").toString());
        }
        return null;
    }

    public String extrairEmail(String token) {
        Map<String, Object> claims = validarToken(token);
        if (claims != null && claims.containsKey("email")) {
            return claims.get("email").toString();
        }
        return null;
    }

    public boolean isTokenValido(String token) {
        return validarToken(token) != null;
    }

    private String criarToken(Long usuarioId, String email, String type, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(usuarioId));
        claims.put("email", email);
        claims.put("type", type);
        claims.put("iat", now);
        claims.put("exp", exp);

        try {
            String payloadJson = objectMapper.writeValueAsString(claims);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = HEADER_B64 + "." + payloadB64;
            String signature = assinar(dataToSign);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar JWT: " + e.getMessage(), e);
        }
    }

    private String assinar(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretKeyBytes, HMAC_SHA256));
            byte[] sigBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Erro criptográfico ao assinar JWT: " + e.getMessage(), e);
        }
    }
}
