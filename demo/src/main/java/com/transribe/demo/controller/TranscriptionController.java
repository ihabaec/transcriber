package com.transribe.demo.controller;

import com.transribe.demo.service.TranscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class TranscriptionController {

    @Autowired
    private TranscriptionService transcriptionService;

    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribe(@RequestParam String youtubeUrl) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate YouTube URL
            if (!isValidYouTubeUrl(youtubeUrl)) {
                response.put("success", false);
                response.put("error", "Invalid YouTube URL");
                return ResponseEntity.badRequest().body(response);
            }

            // Process transcription
            String transcription = transcriptionService.transcribeYouTubeVideo(youtubeUrl);
            
            response.put("success", true);
            response.put("transcription", transcription);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Error processing video: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private boolean isValidYouTubeUrl(String url) {
        return url != null && 
               (url.contains("youtube.com/watch?v=") || 
                url.contains("youtu.be/") ||
                url.contains("youtube.com/embed/"));
    }
}