package com.victorquan.spotify.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class SpotifyTokenService {

    private final RestTemplate restTemplate;

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.refresh-token}")
    private String refreshToken;

    private String accessToken;
    private long tokenExpiresAt = 0;

    public SpotifyTokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAccessToken() {
        if (System.currentTimeMillis() >= tokenExpiresAt - 60_000) {
            refreshAccessToken();
        }
        return accessToken;
    }

    @PostConstruct
    @Scheduled(fixedDelay = 3_300_000) // refresh every ~55 minutes
    public void refreshAccessToken() {
        if (refreshToken == null || refreshToken.isBlank()) {
            return; // No token yet — visit /auth/login to complete OAuth
        }
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token", request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            accessToken = (String) response.getBody().get("access_token");
            int expiresIn = (int) response.getBody().get("expires_in");
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
        }
    }

    /**
     * Updates the in-memory refresh token and immediately fetches a new access token.
     * Called after a successful OAuth callback so the app works without a restart.
     * The caller is still responsible for persisting the token as SPOTIFY_REFRESH_TOKEN.
     */
    public synchronized void updateRefreshToken(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        refreshAccessToken();
    }
}
