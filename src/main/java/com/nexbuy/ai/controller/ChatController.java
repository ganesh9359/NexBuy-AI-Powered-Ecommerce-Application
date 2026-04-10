package com.nexbuy.ai.controller;

import com.nexbuy.ai.dto.AiRequest;
import com.nexbuy.ai.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/search-assist")
    public ResponseEntity<AiRequest.ChatResponse> searchAssist(Authentication authentication,
                                                               @RequestBody AiRequest.ChatPromptRequest request) {
        // For now, return a simple response to prevent 500 errors
        // TODO: Implement proper AI search assistance
        AiRequest.ChatResponse response = new AiRequest.ChatResponse(
            "en", // language
            "Search Assistance", // headline
            "I can help you search for products. Try searching for specific items like 'smartphones', 'laptops', or 'headphones'.", // answer
            "search_help", // intent
            List.of("Show me smartphones", "Find laptops", "Search headphones"), // quickReplies
            List.of(), // products
            List.of(), // orders
            "Continue searching", // nextStep
            null // targetUrl
        );
        return ResponseEntity.ok(response);
    }
}