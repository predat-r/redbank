package com.redmath.redbank.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

  @Bean
  public ChatClient chatClient(ChatClient.Builder builder) {
    return builder.build();
  }

  @Bean("locationRiskChatClient")
  ChatClient locationRiskChatClient(ChatClient.Builder builder) {
    return builder.build();
  }

  /**
   * Lean client for intent parsing — no advisors, no RAG, no memory overhead. Returns deterministic
   * JSON; conversation history is irrelevant for this call.
   */
  @Bean("intentChatClient")
  ChatClient intentChatClient(ChatClient.Builder builder) {
    return builder.build();
  }

  /**
   * Conversational client for natural-language answer phrasing. Carries memory + RAG so replies are
   * contextually aware of the user's history.
   */
  @Bean("conversationChatClient")
  ChatClient conversationChatClient(ChatClient.Builder builder) {
    ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
    Advisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

    return builder
        .defaultAdvisors(memoryAdvisor)
        .build();
  }
}