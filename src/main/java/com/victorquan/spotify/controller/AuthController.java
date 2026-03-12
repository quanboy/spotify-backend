package com.victorquan.spotify.controller;

import com.victorquan.spotify.service.SpotifyTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * ONE-TIME USE: Visit /auth/login to kick off OAuth.
 * After authorizing, the refresh token is displayed on screen — copy it into
 * Railway as SPOTIFY_REFRESH_TOKEN so it survives restarts.
 * The running instance is updated immediately so no restart is required to start serving data.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.callback-url:http://localhost:8080/auth/callback}")
    private String callbackUrl;

    private final RestTemplate restTemplate;
    private final SpotifyTokenService tokenService;

    public AuthController(RestTemplate restTemplate, SpotifyTokenService tokenService) {
        this.restTemplate = restTemplate;
        this.tokenService = tokenService;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String scopes = "user-read-currently-playing user-read-recently-played user-library-read";
        String url = "https://accounts.spotify.com/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&scope=" + scopes.replace(" ", "%20")
                + "&redirect_uri=" + callbackUrl;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code) {
        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", callbackUrl);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token", request, Map.class);

        if (response.getBody() != null) {
            String refreshToken = (String) response.getBody().get("refresh_token");

            // Update the running instance immediately — no restart needed
            tokenService.updateRefreshToken(refreshToken);

            return ResponseEntity.ok(
                "<html><body style='font-family:monospace;padding:2rem'>"
                + "<h2>✅ Authorized!</h2>"
                + "<p>The app is now serving Spotify data. To persist this across restarts, "
                + "set the following as <strong>SPOTIFY_REFRESH_TOKEN</strong> in Railway:</p>"
                + "<pre style='background:#f0f0f0;padding:1rem;border-radius:4px'>" + refreshToken + "</pre>"
                + "</body></html>"
            );
        }

        return ResponseEntity.status(500).body("Authorization failed.");
    }
}
