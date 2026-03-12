package com.victorquan.spotify.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class SpotifyService {

    private final RestTemplate restTemplate;
    private final SpotifyTokenService tokenService;

    private static final String API_BASE = "https://api.spotify.com/v1";

    public SpotifyService(RestTemplate restTemplate, SpotifyTokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + tokenService.getAccessToken());
        return headers;
    }

    private ResponseEntity<Map> spotifyGet(String url) {
        return restTemplate.exchange(url, HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
    }

    // ── NOW PLAYING ──────────────────────────────────────────────────────────
    @Cacheable(value = "nowPlaying")
    public Map<String, Object> getNowPlaying() {
        try {
            ResponseEntity<Map> response = spotifyGet(
                    API_BASE + "/me/player/currently-playing");

            if (response.getStatusCode() == HttpStatus.NO_CONTENT
                    || response.getBody() == null) {
                return Map.of("isPlaying", false);
            }

            Map body = response.getBody();
            boolean isPlaying = (boolean) body.getOrDefault("is_playing", false);
            Map item = (Map) body.get("item");

            if (item == null) return Map.of("isPlaying", false);

            return buildTrackMap(item, isPlaying);

        } catch (HttpClientErrorException.TooManyRequests e) {
            return Map.of("isPlaying", false, "error", "rate_limited");
        } catch (Exception e) {
            return Map.of("isPlaying", false);
        }
    }

    // ── RECENTLY PLAYED ───────────────────────────────────────────────────────
    @Cacheable(value = "recentlyPlayed")
    public List<Map<String, Object>> getRecentlyPlayed() {
        try {
            ResponseEntity<Map> response = spotifyGet(
                    API_BASE + "/me/player/recently-played?limit=6");

            if (response.getBody() == null) return List.of();

            List<Map> items = (List<Map>) response.getBody().get("items");
            List<Map<String, Object>> result = new ArrayList<>();

            for (Map item : items) {
                Map track = (Map) item.get("track");
                String playedAt = (String) item.get("played_at");
                Map<String, Object> trackMap = buildTrackMap(track, false);
                trackMap.put("playedAt", playedAt);
                result.add(trackMap);
            }

            return result;

        } catch (Exception e) {
            return List.of();
        }
    }

    // ── RANDOM TRACK ──────────────────────────────────────────────────────────
    public Map<String, Object> getRandomTrack() {
        try {
            // Get a random saved track using a random offset
            int randomOffset = new Random().nextInt(200);
            ResponseEntity<Map> response = spotifyGet(
                    API_BASE + "/me/tracks?limit=1&offset=" + randomOffset);

            if (response.getBody() == null) return Map.of();

            List<Map> items = (List<Map>) response.getBody().get("items");
            if (items == null || items.isEmpty()) return Map.of();

            Map track = (Map) items.get(0).get("track");
            return buildTrackMap(track, false);

        } catch (Exception e) {
            return Map.of();
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    private Map<String, Object> buildTrackMap(Map item, boolean isPlaying) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isPlaying", isPlaying);
        result.put("title", item.get("name"));
        result.put("id", item.get("id"));

        // Artists
        List<Map> artists = (List<Map>) item.get("artists");
        String artistNames = artists.stream()
                .map(a -> (String) a.get("name"))
                .reduce((a, b) -> a + ", " + b)
                .orElse("Unknown");
        result.put("artist", artistNames);

        // Album
        Map album = (Map) item.get("album");
        if (album != null) {
            result.put("album", album.get("name"));

            List<Map> images = (List<Map>) album.get("images");
            if (images != null && !images.isEmpty()) {
                result.put("albumArt", images.get(0).get("url"));
            }
        }

        // Spotify URL
        Map externalUrls = (Map) item.get("external_urls");
        if (externalUrls != null) {
            result.put("url", externalUrls.get("spotify"));
        }

        // Preview URL
        result.put("previewUrl", item.get("preview_url"));

        // Duration
        result.put("durationMs", item.get("duration_ms"));

        return result;
    }
}
