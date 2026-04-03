package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.VoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/voice-search")
    public ResponseEntity<AiRequest.VoiceSearchResponse> search(Authentication authentication,
                                                                @RequestBody AiRequest.VoiceSearchRequest request) {
        return ResponseEntity.ok(voiceService.search(authentication == null ? null : authentication.getName(), request));
    }

    @PostMapping("/search-assist")
    public ResponseEntity<AiRequest.VoiceSearchResponse> assist(Authentication authentication,
                                                                @RequestBody AiRequest.VoiceSearchRequest request) {
        return ResponseEntity.ok(voiceService.search(authentication == null ? null : authentication.getName(), request));
    }
}
