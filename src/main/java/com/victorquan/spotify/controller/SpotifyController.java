package com.victorquan.spotify.controller;

import com.victorquan.spotify.service.SpotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spotify")
public class SpotifyController {

    private final SpotifyService spotifyService;

    public SpotifyController(SpotifyService spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/now-playing")
    public ResponseEntity<Map<String, Object>> nowPlaying() {
        return ResponseEntity.ok(spotifyService.getNowPlaying());
    }

    @GetMapping("/recently-played")
    public ResponseEntity<List<Map<String, Object>>> recentlyPlayed() {
        return ResponseEntity.ok(spotifyService.getRecentlyPlayed());
    }

    @GetMapping("/random-track")
    public ResponseEntity<Map<String, Object>> randomTrack() {
        return ResponseEntity.ok(spotifyService.getRandomTrack());
    }
}
