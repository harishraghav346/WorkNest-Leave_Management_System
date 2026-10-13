package com.examly.springapp.controller;

import com.examly.springapp.model.AiChatRequest;
import com.examly.springapp.model.AiChatResponse;
import com.examly.springapp.model.AiImpactResponse;
import com.examly.springapp.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chatWithAssistant(@RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiService.processChat(request));
    }

    @GetMapping("/impact/{leaveId}")
    public ResponseEntity<AiImpactResponse> getImpactAnalysis(@PathVariable Long leaveId) {
        return ResponseEntity.ok(aiService.analyzeImpact(leaveId));
    }

    @GetMapping("/ooo/{leaveId}")
    public ResponseEntity<Map<String, String>> getOutOfOfficeTemplate(@PathVariable Long leaveId) {
        String draft = aiService.generateOutOfOffice(leaveId);
        return ResponseEntity.ok(Map.of("template", draft));
    }
}
