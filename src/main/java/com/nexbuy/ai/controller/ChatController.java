package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiRequest.ChatResponse> chat(Authentication authentication,
                                                       @RequestBody AiRequest.ChatPromptRequest request) {
        return ResponseEntity.ok(chatService.chat(authentication == null ? null : authentication.getName(), request));
    }
}