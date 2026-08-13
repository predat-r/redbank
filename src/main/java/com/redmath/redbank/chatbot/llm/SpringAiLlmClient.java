package com.redmath.redbank.chatbot.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.dto.LlmIntentOutput;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class SpringAiLlmClient implements LlmClient {

  private static final String PHRASING_SYSTEM_PROMPT = """
      You are a friendly banking assistant. The user asked a question about their finances.
      You have already looked up the factual answer from the database.
      Your job is to rephrase the raw factual data into a warm, concise, natural-language reply.
      Rules:
      - Do NOT make up any numbers or facts not present in the raw data provided.
      - Keep the reply short (1-3 sentences).
      - Use a friendly, professional tone.
      - Do NOT add disclaimers, suggestions, or advice unless directly relevant.
      - Output ONLY the plain text reply. Do NOT output JSON format.
      """;

  /**
   * No advisors — purely for fast, deterministic JSON intent extraction.
   */
  private final ChatClient intentChatClient;

  /**
   * Memory + RAG advisors — for contextual, conversational answer phrasing.
   */
  private final ChatClient conversationChatClient;

  public SpringAiLlmClient(
      @Qualifier("intentChatClient") ChatClient intentChatClient,
      @Qualifier("conversationChatClient") ChatClient conversationChatClient) {
    this.intentChatClient = intentChatClient;
    this.conversationChatClient = conversationChatClient;
  }

  @Override
  public FinancialQueryIntent parseIntent(String userMessage, LocalDate today,
      String conversationId) {
    String systemPrompt = IntentPromptBuilder.buildSystemPrompt(today);

    String convId = (conversationId != null && !conversationId.isBlank()) ? conversationId : "default";

    // intentChatClient uses memory so it can handle follow-up questions
    LlmIntentOutput output = intentChatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", convId))
        .system(systemPrompt)
        .user(userMessage)
        .call()
        .entity(LlmIntentOutput.class);

    if (output == null) {
      throw new IllegalStateException("Failed to parse intent: LLM returned null output");
    }

    return mapToFinancialQueryIntent(output);
  }

  private FinancialQueryIntent mapToFinancialQueryIntent(LlmIntentOutput output) {
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setQueryType(output.getQueryType());
    intent.setMetric(output.getMetric());
    intent.setDirection(output.getDirection());
    intent.setCategory(output.getCategory());
    intent.setStartDate(output.getStartDate());
    intent.setEndDate(output.getEndDate());
    intent.setAsOfDate(output.getAsOfDate());
    intent.setCounterpartyName(output.getCounterpartyName());
    intent.setNeedsClarification(output.isNeedsClarification());
    intent.setClarificationQuestion(output.getClarificationQuestion());
    intent.setTransactionType(output.getTransactionType());
    intent.setSortOrder(output.getSortOrder());
    return intent;
  }

  @Override
  public FinancialQueryIntent parseIntent(String userMessage, LocalDate today) {
    return parseIntent(userMessage, today, "default");
  }

  @Override
  public String phraseAnswer(String userMessage, String rawFactualAnswer, String conversationId) {
    String convId =
        (conversationId != null && !conversationId.isBlank()) ? conversationId : "default";

    String userPrompt = ("User's original question: " + userMessage + "\n\n"
        + "Raw factual data fetched from the database:\n"
        + rawFactualAnswer + "\n\n"
        + "Please rephrase this into a natural, friendly response.");

    String response = conversationChatClient.prompt()
        .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", convId))
        .system(PHRASING_SYSTEM_PROMPT)
        .user(userPrompt)
        .call()
        .content();

    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(response);
      if (node.has("response")) {
        return node.get("response").asText();
      }
    } catch (JsonProcessingException e) {
      // Not JSON or no "response" field, proceed to return the raw string
    }

    return response;
  }

  @Override
  public String phraseAnswer(String userMessage, String rawFactualAnswer) {
    return phraseAnswer(userMessage, rawFactualAnswer, "default");
  }
}
