package com.victorquan.spotify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyTokenServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SpotifyTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new SpotifyTokenService(restTemplate);
        ReflectionTestUtils.setField(tokenService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(tokenService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(tokenService, "refreshToken", "test-refresh-token");
    }

    @Test
    void refreshAccessToken_storesAccessTokenAndExpiry() {
        stubTokenResponse("new-access-token", 3600);

        tokenService.refreshAccessToken();

        assertEquals("new-access-token", ReflectionTestUtils.getField(tokenService, "accessToken"));
        long expiresAt = (long) ReflectionTestUtils.getField(tokenService, "tokenExpiresAt");
        long expectedMin = System.currentTimeMillis() + 3_540_000; // ~59 min from now
        assert expiresAt >= expectedMin : "tokenExpiresAt should be ~1 hour in the future";
    }

    @Test
    void getAccessToken_whenTokenValid_returnsWithoutRefresh() {
        ReflectionTestUtils.setField(tokenService, "accessToken", "valid-token");
        ReflectionTestUtils.setField(tokenService, "tokenExpiresAt",
                System.currentTimeMillis() + 3_600_000);

        String token = tokenService.getAccessToken();

        assertEquals("valid-token", token);
        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }

    @Test
    void getAccessToken_whenExpiringSoon_triggersRefresh() {
        ReflectionTestUtils.setField(tokenService, "accessToken", "expiring-token");
        // Expires in 30s — within the 60s refresh threshold
        ReflectionTestUtils.setField(tokenService, "tokenExpiresAt",
                System.currentTimeMillis() + 30_000);

        stubTokenResponse("refreshed-token", 3600);

        String token = tokenService.getAccessToken();

        assertEquals("refreshed-token", token);
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    @Test
    void getAccessToken_whenTokenExpired_triggersRefresh() {
        ReflectionTestUtils.setField(tokenService, "accessToken", "expired-token");
        ReflectionTestUtils.setField(tokenService, "tokenExpiresAt",
                System.currentTimeMillis() - 1_000);

        stubTokenResponse("fresh-token", 3600);

        String token = tokenService.getAccessToken();

        assertEquals("fresh-token", token);
    }

    @Test
    void updateRefreshToken_replacesRefreshTokenAndRefreshesAccess() {
        stubTokenResponse("post-update-token", 3600);

        tokenService.updateRefreshToken("brand-new-refresh-token");

        assertEquals("brand-new-refresh-token",
                ReflectionTestUtils.getField(tokenService, "refreshToken"));
        assertEquals("post-update-token", ReflectionTestUtils.getField(tokenService, "accessToken"));
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubTokenResponse(String accessToken, int expiresIn) {
        Map<String, Object> body = Map.of(
                "access_token", accessToken,
                "expires_in", expiresIn
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }
}
