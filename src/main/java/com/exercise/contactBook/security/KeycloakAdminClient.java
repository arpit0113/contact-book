package com.exercise.contactBook.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakAdminClient {
    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient http = RestClient.create();

    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String webClientId;
    private final String frontendUrl;

    private String token;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public KeycloakAdminClient(
                               @Value("${keycloak.internal-url}") String serverUrl,
                               @Value("${keycloak.realm}") String realm,
                               @Value("${keycloak.admin.client-id}") String clientId,
                               @Value("${keycloak.admin.client-secret:}") String clientSecret,
                               @Value("${keycloak.web-client-id}") String webClientId,
                               @Value("${app.frontend-url}") String frontendUrl) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClientId = webClientId;
        this.frontendUrl = frontendUrl;
    }

    public Optional<String> findUserIdByEmail(String email) {
        List<Map<String, Object>> users = call("find user", () -> http.get()
                .uri(adminBase() + "/users?email={email}&exact=true", email)
                .header("Authorization", "Bearer " + accessToken())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {}));
        if (users == null || users.isEmpty()) return Optional.empty();
        return Optional.of(String.valueOf(users.get(0).get("id")));
    }


    public String createUser(String email, String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        int space = trimmed.indexOf(' ');
        String first = space > 0 ? trimmed.substring(0, space) : (trimmed.isEmpty() ? email : trimmed);
        String last = space > 0 ? trimmed.substring(space + 1).trim() : "-";

        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", first,
                "lastName", last.isEmpty() ? "-" : last,
                "enabled", true,
                "emailVerified", true);

        ResponseEntity<Void> response = call("create user", () -> http.post()
                .uri(adminBase() + "/users")
                .header("Authorization", "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity());

        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider did not return the new user id");
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }


    public void sendSetPasswordEmail(String userId) {
        call("send set-password email", () -> http.put()
                .uri(adminBase() + "/users/{id}/execute-actions-email?client_id={cid}&redirect_uri={uri}&lifespan=259200",
                        userId, webClientId, frontendUrl + "/")
                .header("Authorization", "Bearer " + accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD"))
                .retrieve()
                .toBodilessEntity());
    }

    public void deleteUser(String userId) {
        try {
            http.delete()
                    .uri(adminBase() + "/users/{id}", userId)
                    .header("Authorization", "Bearer " + accessToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Keycloak user {} was already gone", userId);
        } catch (RestClientResponseException e) {
            log.error("Keycloak admin call 'delete user' failed: HTTP {} {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider error while trying to delete user");
        } catch (RestClientException e) {
            log.error("Keycloak admin call 'delete user' failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider is unreachable");
        }
    }

    // helperrrsssssssss

    private String adminBase() {
        return serverUrl + "/admin/realms/" + realm;
    }

    private synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(30))) {
            return token;
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Identity provider admin client is not configured (keycloak.admin.client-secret)");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> response = call("get admin token", () -> http.post()
                .uri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {}));

        token = String.valueOf(response.get("access_token"));
        long expiresIn = response.get("expires_in") instanceof Number n ? n.longValue() : 60;
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
        return token;
    }

    private <T> T call(String what, java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            log.error("Keycloak admin call '{}' failed: HTTP {} {}", what, e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider error while trying to " + what);
        } catch (RestClientException e) {
            log.error("Keycloak admin call '{}' failed: {}", what, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Identity provider is unreachable");
        }
    }
}
