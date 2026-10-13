package com.examly.springapp.service;

import com.examly.springapp.model.AiChatRequest;
import com.examly.springapp.model.AiChatResponse;
import com.examly.springapp.model.AiImpactResponse;

public interface AiService {
    AiChatResponse processChat(AiChatRequest request);
    AiImpactResponse analyzeImpact(Long leaveId);
    String generateOutOfOffice(Long leaveId);
}
