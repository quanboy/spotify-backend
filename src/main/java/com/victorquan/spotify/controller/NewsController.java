package com.victorquan.spotify.controller;

import com.victorquan.spotify.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/headlines")
    public ResponseEntity<List<String>> headlines() {
        return ResponseEntity.ok(newsService.getHeadlines());
    }
}
