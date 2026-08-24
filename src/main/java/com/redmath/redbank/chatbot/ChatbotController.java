package com.redmath.redbank.chatbot;

import com.redmath.redbank.chatbot.dto.ChatRequest;
import com.redmath.redbank.chatbot.dto.ChatResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/me/chat")
public class ChatbotController {

  private final ChatbotService chatbotService;

  public ChatbotController(ChatbotService chatbotService) {
    this.chatbotService = chatbotService;
  }

  @PostMapping(consumes = "application/json")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public ChatResponse chat(@Valid @RequestBody ChatRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    Long userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }

    ChatResponse response = chatbotService.handle(request.getMessage(), userId);
    return response;
  }
}