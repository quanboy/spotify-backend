package com.victorquan.spotify.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private final RestTemplate restTemplate;

    @Value("${news.api-key}")
    private String apiKey;

    public NewsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Cacheable("headlines")
    @SuppressWarnings("unchecked")
    public List<String> getHeadlines() {
        if (apiKey == null || apiKey.isBlank()) return Collections.emptyList();
        try {
            String url = "https://newsapi.org/v2/top-headlines?q=trump&country=us&pageSize=15&apiKey=" + apiKey;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || !response.containsKey("articles")) return Collections.emptyList();
            List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
            return articles.stream()
                    .map(a -> (String) a.get("title"))
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
