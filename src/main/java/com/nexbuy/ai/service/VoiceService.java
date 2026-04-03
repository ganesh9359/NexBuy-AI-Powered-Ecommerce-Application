package com.nexbuy.ai.service;

import com.nexbuy.ai.dto.AiRequest;

public interface VoiceService {
    AiRequest.VoiceSearchResponse search(String email, AiRequest.VoiceSearchRequest request);
}