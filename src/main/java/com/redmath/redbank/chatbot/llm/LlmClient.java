package com.redmath.redbank.chatbot.llm;

import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import java.time.LocalDate;

public interface LlmClient {

  FinancialQueryIntent parseIntent(String userMessage, LocalDate today, String conversationId);

  FinancialQueryIntent parseIntent(String userMessage, LocalDate today);

  String phraseAnswer(String userMessage, String rawFactualAnswer, String conversationId);

  String phraseAnswer(String userMessage, String rawFactualAnswer);
}