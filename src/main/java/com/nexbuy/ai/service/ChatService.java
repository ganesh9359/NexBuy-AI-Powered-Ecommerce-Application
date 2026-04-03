package com.nexbuy.ai.service;

import com.nexbuy.ai.dto.AiRequest;

public interface ChatService {
    AiRequest.ChatResponse chat(String email, AiRequest.ChatPromptRequest request);
}