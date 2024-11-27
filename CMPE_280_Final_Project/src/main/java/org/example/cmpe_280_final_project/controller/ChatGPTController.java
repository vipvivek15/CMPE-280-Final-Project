package org.example.cmpe_280_final_project.controller;

import org.example.cmpe_280_final_project.service.ChatGPTService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatgpt")
public class ChatGPTController {

    @Autowired
    private ChatGPTService gptService;

    // Endpoint to handle user prompts
    @PostMapping
    public String processPrompt(@RequestBody String userPrompt) {
        return gptService.handleUserPrompt(userPrompt);
    }
}

