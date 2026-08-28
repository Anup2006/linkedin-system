package com.linkedin.feed_service.controller;

import com.linkedin.feed_service.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;

    //Get paginated feed for a user
    //return list of post ids
    //client fetched full post details from post service
    @GetMapping("/{userId}")
    public ResponseEntity<List<String>> getFeed(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        return ResponseEntity.ok(feedService.getFeed(userId,page,size));
    }

    //clear feed cache - useful for testing
    @DeleteMapping("/{userId}/cache")
    public ResponseEntity<String> clearFeed(
            @PathVariable String userId
    ){
        feedService.clearFeed(userId);
        return ResponseEntity.ok("Feed cache cleared");
    }


}
