package com.redmath.redbank.chatbot.llm;

import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringAiLlmClient implements LlmClient {

  private final ChatClient chatClient;

  public SpringAiLlmClient(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public FinancialQueryIntent parseIntent(String userMessage, LocalDate today, String conversationId) {
    String systemPrompt = IntentPromptBuilder.buildSystemPrompt(today);
    String convId = (conversationId != null && !conversationId.isBlank()) ? conversationId : "default";

    return chatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", convId))
        .system(systemPrompt)
        .user(userMessage)
        .call()
        .entity(FinancialQueryIntent.class);
  }

  @Override
  public FinancialQueryIntent parseIntent(String userMessage, LocalDate today) {
    return parseIntent(userMessage, today, "default");
  }

  @Override
  public String phraseAnswer(String userMessage, String rawFactualAnswer) {
    // Optional: only call this for open-ended phrasing.
    // For most intents, template the response directly instead (cheaper, zero hallucination risk).
    return rawFactualAnswer;
  }
}
