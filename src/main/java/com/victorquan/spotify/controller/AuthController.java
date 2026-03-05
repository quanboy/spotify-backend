package com.victorquan.spotify.controller;

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
 * After authorizing, you'll get your refresh token printed to the screen.
 * Copy it into Railway as SPOTIFY_REFRESH_TOKEN, then you can ignore this controller.
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

    public AuthController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            return ResponseEntity.ok(
                "<html><body style='font-family:monospace;padding:2rem'>"
                + "<h2>✅ Authorized!</h2>"
                + "<p>Copy this refresh token into Railway as <strong>SPOTIFY_REFRESH_TOKEN</strong>:</p>"
                + "<pre style='background:#f0f0f0;padding:1rem;border-radius:4px'>" + refreshToken + "</pre>"
                + "</body></html>"
            );
        }

        return ResponseEntity.status(500).body("Authorization failed.");
    }
}
