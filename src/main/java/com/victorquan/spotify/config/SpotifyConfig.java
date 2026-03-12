package com.victorquan.spotify.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class SpotifyConfig implements WebMvcConfigurer {

    @Value("${spotify.allowed-origins}")
    private String allowedOrigins;

    @Value("${spotify.cache.now-playing-ttl}")
    private int nowPlayingTtl;

    @Value("${spotify.cache.recently-played-ttl}")
    private int recentlyPlayedTtl;

    @Value("${news.cache.headlines-ttl-seconds}")
    private int headlinesTtl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
            new CaffeineCache("nowPlaying",
                Caffeine.newBuilder().expireAfterWrite(nowPlayingTtl, TimeUnit.SECONDS).build()),
            new CaffeineCache("recentlyPlayed",
                Caffeine.newBuilder().expireAfterWrite(recentlyPlayedTtl, TimeUnit.SECONDS).build()),
            new CaffeineCache("headlines",
                Caffeine.newBuilder().expireAfterWrite(headlinesTtl, TimeUnit.SECONDS).build())
        ));
        return manager;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/spotify/**")
                .allowedOrigins(origins)
                .allowedMethods("GET")
                .maxAge(3600);
        registry.addMapping("/news/**")
                .allowedOrigins(origins)
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
