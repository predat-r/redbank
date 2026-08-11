package com.redmath.redbank.chatbot.llm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IntentPromptBuilderTest {

  @Test
  void testBuildSystemPromptContainsDate() {
    LocalDate today = LocalDate.of(2026, 8, 11);
    String prompt = IntentPromptBuilder.buildSystemPrompt(today);
    
    assertTrue(prompt.contains("2026-08-11"), "System prompt should contain the current date.");
    assertTrue(prompt.contains("You are a query parser for a banking app"), "System prompt should contain the expected persona.");
  }
}
