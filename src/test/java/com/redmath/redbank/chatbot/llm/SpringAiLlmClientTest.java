package com.redmath.redbank.chatbot.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.dto.LlmIntentOutput;
import com.redmath.redbank.chatbot.enums.QueryType;
import java.time.LocalDate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class SpringAiLlmClientTest {

  private ChatClient intentChatClient;
  private ChatClient conversationChatClient;
  private SpringAiLlmClient llmClient;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    intentChatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    conversationChatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    objectMapper = new ObjectMapper();
    llmClient = new SpringAiLlmClient(intentChatClient, conversationChatClient, objectMapper);
  }

  @Test
  void parseIntentSuccess() {
    LlmIntentOutput mockOutput = new LlmIntentOutput();
    mockOutput.setQueryType(QueryType.BALANCE_AT_DATE);
    
    when(intentChatClient.prompt()
        .advisors(any(java.util.function.Consumer.class))
        .system(anyString())
        .user(anyString())
        .call()
        .entity(LlmIntentOutput.class)).thenReturn(mockOutput);

    FinancialQueryIntent intent = llmClient.parseIntent("What is my balance?", LocalDate.now(), "conv-1");
    
    assertNotNull(intent);
    assertEquals(QueryType.BALANCE_AT_DATE, intent.getQueryType());
  }

  @Test
  void phraseAnswerSuccess() {
    when(conversationChatClient.prompt()
        .advisors(any(java.util.function.Consumer.class))
        .system(anyString())
        .user(any(java.util.function.Consumer.class))
        .call()
        .content()).thenReturn("Your balance is $100.00.");

    String response = llmClient.phraseAnswer("balance?", "Facts: 100", "conv-1");
    
    assertEquals("Your balance is $100.00.", response);
  }
}
