package com.victorquan.spotify.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpotifyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SpotifyTokenService tokenService;

    @InjectMocks
    private SpotifyService spotifyService;

    @BeforeEach
    void setUp() {
        when(tokenService.getAccessToken()).thenReturn("test-access-token");
    }

    // ── getNowPlaying ─────────────────────────────────────────────────────────

    @Test
    void getNowPlaying_whenPlaying_returnsPopulatedTrackMap() {
        when(restTemplate.exchange(contains("/me/player/currently-playing"),
                eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(buildCurrentlyPlayingResponse(true)));

        Map<String, Object> result = spotifyService.getNowPlaying();

        assertTrue((Boolean) result.get("isPlaying"));
        assertEquals("Test Track", result.get("title"));
        assertEquals("Test Artist", result.get("artist"));
        assertEquals("Test Album", result.get("album"));
        assertNotNull(result.get("albumArt"));
        assertNotNull(result.get("url"));
    }

    @Test
    void getNowPlaying_whenNotPlaying_returnsIsPlayingFalse() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(buildCurrentlyPlayingResponse(false)));

        Map<String, Object> result = spotifyService.getNowPlaying();

        assertFalse((Boolean) result.get("isPlaying"));
    }

    @Test
    void getNowPlaying_whenNoContent_returnsIsPlayingFalse() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.noContent().build());

        Map<String, Object> result = spotifyService.getNowPlaying();

        assertFalse((Boolean) result.get("isPlaying"));
    }

    @Test
    void getNowPlaying_whenRateLimited_returnsRateLimitedError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        Map<String, Object> result = spotifyService.getNowPlaying();

        assertFalse((Boolean) result.get("isPlaying"));
        assertEquals("rate_limited", result.get("error"));
    }

    @Test
    void getNowPlaying_onGenericException_returnsIsPlayingFalseNoError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        Map<String, Object> result = spotifyService.getNowPlaying();

        assertFalse((Boolean) result.get("isPlaying"));
        assertNull(result.get("error"));
    }

    // ── getRecentlyPlayed ─────────────────────────────────────────────────────

    @Test
    void getRecentlyPlayed_returnsTracksWithPlayedAt() {
        when(restTemplate.exchange(contains("/me/player/recently-played"),
                eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(buildRecentlyPlayedResponse(3)));

        List<Map<String, Object>> result = spotifyService.getRecentlyPlayed();

        assertEquals(3, result.size());
        assertEquals("Track 0", result.get(0).get("title"));
        assertNotNull(result.get(0).get("playedAt"));
    }

    @Test
    void getRecentlyPlayed_onException_returnsEmptyList() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        List<Map<String, Object>> result = spotifyService.getRecentlyPlayed();

        assertTrue(result.isEmpty());
    }

    @Test
    void getRecentlyPlayed_whenNullBody_returnsEmptyList() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(null));

        List<Map<String, Object>> result = spotifyService.getRecentlyPlayed();

        assertTrue(result.isEmpty());
    }

    // ── getRandomTrack ────────────────────────────────────────────────────────

    @Test
    void getRandomTrack_returnsTrack() {
        when(restTemplate.exchange(contains("/me/tracks"),
                eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(buildSavedTracksResponse()));

        Map<String, Object> result = spotifyService.getRandomTrack();

        assertEquals("Saved Track", result.get("title"));
        assertEquals("Saved Artist", result.get("artist"));
    }

    @Test
    void getRandomTrack_onException_returnsEmptyMap() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        Map<String, Object> result = spotifyService.getRandomTrack();

        assertTrue(result.isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> buildCurrentlyPlayingResponse(boolean isPlaying) {
        Map<String, Object> image = new HashMap<>();
        image.put("url", "https://example.com/art.jpg");

        Map<String, Object> album = new HashMap<>();
        album.put("name", "Test Album");
        album.put("images", List.of(image));

        Map<String, Object> artist = new HashMap<>();
        artist.put("name", "Test Artist");

        Map<String, Object> externalUrls = new HashMap<>();
        externalUrls.put("spotify", "https://open.spotify.com/track/123");

        Map<String, Object> item = new HashMap<>();
        item.put("id", "track123");
        item.put("name", "Test Track");
        item.put("artists", List.of(artist));
        item.put("album", album);
        item.put("external_urls", externalUrls);
        item.put("preview_url", "https://preview.example.com/30s.mp3");
        item.put("duration_ms", 210000);

        Map<String, Object> response = new HashMap<>();
        response.put("is_playing", isPlaying);
        response.put("item", item);
        return response;
    }

    private Map<String, Object> buildRecentlyPlayedResponse(int count) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> image = new HashMap<>();
            image.put("url", "https://example.com/art" + i + ".jpg");

            Map<String, Object> album = new HashMap<>();
            album.put("name", "Album " + i);
            album.put("images", List.of(image));

            Map<String, Object> artist = new HashMap<>();
            artist.put("name", "Artist " + i);

            Map<String, Object> externalUrls = new HashMap<>();
            externalUrls.put("spotify", "https://open.spotify.com/track/" + i);

            Map<String, Object> track = new HashMap<>();
            track.put("id", "track" + i);
            track.put("name", "Track " + i);
            track.put("artists", List.of(artist));
            track.put("album", album);
            track.put("external_urls", externalUrls);
            track.put("preview_url", null);
            track.put("duration_ms", 200000);

            Map<String, Object> entry = new HashMap<>();
            entry.put("track", track);
            entry.put("played_at", "2026-03-12T10:0" + i + ":00Z");
            items.add(entry);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        return response;
    }

    private Map<String, Object> buildSavedTracksResponse() {
        Map<String, Object> image = new HashMap<>();
        image.put("url", "https://example.com/saved-art.jpg");

        Map<String, Object> album = new HashMap<>();
        album.put("name", "Saved Album");
        album.put("images", List.of(image));

        Map<String, Object> artist = new HashMap<>();
        artist.put("name", "Saved Artist");

        Map<String, Object> externalUrls = new HashMap<>();
        externalUrls.put("spotify", "https://open.spotify.com/track/saved");

        Map<String, Object> track = new HashMap<>();
        track.put("id", "savedtrack");
        track.put("name", "Saved Track");
        track.put("artists", List.of(artist));
        track.put("album", album);
        track.put("external_urls", externalUrls);
        track.put("preview_url", null);
        track.put("duration_ms", 180000);

        Map<String, Object> entry = new HashMap<>();
        entry.put("track", track);

        Map<String, Object> response = new HashMap<>();
        response.put("items", List.of(entry));
        return response;
    }
}
