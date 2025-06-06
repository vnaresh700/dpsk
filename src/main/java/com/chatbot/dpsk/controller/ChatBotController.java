package com.chatbot.dpsk.controller;

import com.chatbot.dpsk.service.TogetherAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChatBotController {

    @Autowired
    private TogetherAIService togetherAIService;

    @Autowired
    public ChatBotController(TogetherAIService togetherAIService) {
        this.togetherAIService = togetherAIService;
    }

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }

    @PostMapping("/ask")
    public String askQuestion(@RequestParam("userInput") String userInput, Model model) {
        String aiResponse = togetherAIService.getAIResponse(userInput);

        model.addAttribute("userInput", userInput);
        model.addAttribute("aiResponse", aiResponse);
        return "chat";
    }
}