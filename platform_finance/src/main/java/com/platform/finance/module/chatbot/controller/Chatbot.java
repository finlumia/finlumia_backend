package com.platform.finance.module.chatbot.controller;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class Chatbot {
    private final OllamaChatModel chatModel;

    public Chatbot(OllamaChatModel chatModel){
        this.chatModel = chatModel;
    }



}
