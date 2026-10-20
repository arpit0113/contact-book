package com.exercise.contactBook.service;
import com.exercise.contactBook.model.RefreshToken;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String issueNewToken(User user) {
        String family = UUID.randomUUID().toString();
        return createAndSaveToken(user, family);
    }

    public String rotate(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeFamily(existing.getFamily());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected, please log in again");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createAndSaveToken(existing.getUser(), existing.getFamily());
    }

    public User getUserForToken(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        return token.getUser();
    }

    public void revoke(String rawToken) {
        String hash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> refreshTokenRepository.revokeFamily(token.getFamily()));
    }

    private String createAndSaveToken(User user, String family) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamily(family);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        return rawToken;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(value.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
