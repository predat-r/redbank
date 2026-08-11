package com.redmath.redbank.chatbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.chatbot.dto.ChatResponse;
import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.enums.QueryType;
import com.redmath.redbank.chatbot.llm.LlmClient;
import com.redmath.redbank.chatbot.query.BalanceQueryService;
import com.redmath.redbank.chatbot.query.CounterpartyResolver;
import com.redmath.redbank.chatbot.query.TransactionQueryService;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

  @Mock
  private LlmClient llmClient;

  @Mock
  private TransactionQueryService transactionQueryService;

  @Mock
  private BalanceQueryService balanceQueryService;

  @Mock
  private CounterpartyResolver counterpartyResolver;

  @Mock
  private AccountHolderService accountHolderService;

  @InjectMocks
  private ChatbotService chatbotService;

  private AccountHolder accountHolder;

  @BeforeEach
  void setUp() {
    accountHolder = new AccountHolder();
    accountHolder.setId(10L);
    accountHolder.setAccountNumber("RB-1234");
  }

  @Test
  void handleNullUserId() {
    ChatResponse response = chatbotService.handle("Hello", null);
    assertFalse(response.isNeedsClarification());
    assertTrue(response.getReply().contains("identify your account"));
  }

  @Test
  void handleAccountHolderNotFound() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenThrow(new RuntimeException());
    ChatResponse response = chatbotService.handle("Hello", 1L);
    assertFalse(response.isNeedsClarification());
    assertTrue(response.getReply().contains("couldn't load your account"));
  }

  @Test
  void handleNeedsClarification() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenReturn(accountHolder);
    
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setNeedsClarification(true);
    intent.setClarificationQuestion("Did you mean debit or credit?");
    when(llmClient.parseIntent(anyString(), any(LocalDate.class), anyString())).thenReturn(intent);
    
    ChatResponse response = chatbotService.handle("test", 1L);
    assertTrue(response.isNeedsClarification());
    assertEquals("Did you mean debit or credit?", response.getReply());
  }

  @Test
  void handleAdviceRequest() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenReturn(accountHolder);
    
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setQueryType(QueryType.ADVICE_REQUEST);
    when(llmClient.parseIntent(anyString(), any(LocalDate.class), anyString())).thenReturn(intent);
    
    ChatResponse response = chatbotService.handle("Should I buy this?", 1L);
    assertFalse(response.isNeedsClarification());
    assertTrue(response.getReply().contains("can't give personal financial advice"));
  }

  @Test
  void handleBalanceAtDate() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenReturn(accountHolder);
    
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setQueryType(QueryType.BALANCE_AT_DATE);
    intent.setAsOfDate(LocalDate.now());
    when(llmClient.parseIntent(anyString(), any(LocalDate.class), anyString())).thenReturn(intent);
    when(balanceQueryService.getBalanceAsOf(anyLong(), any(LocalDate.class))).thenReturn(Optional.of(new BigDecimal("500.00")));
    
    ChatResponse response = chatbotService.handle("What is my balance?", 1L);
    assertFalse(response.isNeedsClarification());
    assertTrue(response.getReply().contains("$500.00"));
  }

  @Test
  void handleProjection() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenReturn(accountHolder);
    
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setQueryType(QueryType.PROJECTION);
    when(llmClient.parseIntent(anyString(), any(LocalDate.class), anyString())).thenReturn(intent);
    when(balanceQueryService.projectMonthEndBalance(anyLong())).thenReturn(new BigDecimal("1000.00"));
    when(llmClient.phraseAnswer(anyString(), anyString(), anyString())).thenReturn("Your projected balance is $1000.00");
    
    ChatResponse response = chatbotService.handle("Project balance", 1L);
    assertFalse(response.isNeedsClarification());
    assertEquals("Your projected balance is $1000.00", response.getReply());
  }

  @Test
  void handleTransactionLookup() {
    when(accountHolderService.getAccountHolderByUserId(1L)).thenReturn(accountHolder);
    
    FinancialQueryIntent intent = new FinancialQueryIntent();
    intent.setQueryType(QueryType.TRANSACTION_LOOKUP);
    intent.setTransactionType(TransactionType.WITHDRAWAL);
    intent.setSortOrder("LATEST");
    when(llmClient.parseIntent(anyString(), any(LocalDate.class), anyString())).thenReturn(intent);
    
    BankTransaction txn = new BankTransaction();
    txn.setAmount(new BigDecimal("50.00"));
    txn.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    txn.setTransactionReference("REF-123");
    when(transactionQueryService.findLatestOrEarliest(any(FinancialQueryIntent.class), anyLong())).thenReturn(Optional.of(txn));
    
    ChatResponse response = chatbotService.handle("Last withdrawal", 1L);
    assertFalse(response.isNeedsClarification());
    assertTrue(response.getReply().contains("$50.00"));
    assertTrue(response.getReply().contains("REF-123"));
  }
}
