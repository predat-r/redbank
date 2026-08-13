package com.redmath.redbank.ai.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.anomaly.AnomalyAnalysisService;
import com.redmath.redbank.anomaly.AnomalyFlag;
import com.redmath.redbank.anomaly.AnomalyReport;
import com.redmath.redbank.anomaly.AnomalyReportRepository;
import com.redmath.redbank.anomaly.RuleEvaluationResult;
import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionCategory;
import com.redmath.redbank.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class AnomalyAnalysisServiceTest {

  @Mock
  private ChatClient chatClient;

  @Mock
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock
  private ChatClient.CallResponseSpec callResponseSpec;

  @Mock
  private AnomalyReportRepository anomalyReportRepository;

  @Mock
  private BankTransactionRepository bankTransactionRepository;

  private AnomalyAnalysisService anomalyAnalysisService;

  @BeforeEach
  void setUp() {
    anomalyAnalysisService = new AnomalyAnalysisService(
        chatClient, anomalyReportRepository, bankTransactionRepository);
  }

  @Test
  @DisplayName("analyzeAndReport with successful LLM response and behavioral context")
  void analyzeAndReportSuccessWithHistory() {
    AccountHolder source = new AccountHolder();
    source.setId(1L);

    BankTransaction currentTxn = new BankTransaction();
    currentTxn.setId(100L);
    currentTxn.setTransactionReference("TXN-100");
    currentTxn.setSourceAccountHolder(source);
    currentTxn.setType(TransactionType.TRANSFER);
    currentTxn.setCategory(TransactionCategory.FOOD);
    currentTxn.setAmount(new BigDecimal("500.00"));

    BankTransaction pastTxn1 = new BankTransaction();
    pastTxn1.setId(10L);
    pastTxn1.setAmount(new BigDecimal("50.00"));
    pastTxn1.setCategory(TransactionCategory.FOOD);
    pastTxn1.setCreatedAt(OffsetDateTime.of(2026, 8, 10, 12, 0, 0, 0, ZoneOffset.UTC));

    BankTransaction pastTxn2 = new BankTransaction();
    pastTxn2.setId(20L);
    pastTxn2.setAmount(new BigDecimal("150.00"));
    pastTxn2.setCategory(TransactionCategory.GROCERY);
    pastTxn2.setCreatedAt(OffsetDateTime.of(2026, 8, 9, 14, 0, 0, 0, ZoneOffset.UTC));

    RuleEvaluationResult ruleResult = new RuleEvaluationResult();
    ruleResult.addReason("High amount", 40);
    ruleResult.setAnomalyFlag(AnomalyFlag.HIGH);

    when(bankTransactionRepository.findBySourceAccountHolderIdAndCreatedAtAfterOrderByCreatedAtDesc(
        eq(1L), any(OffsetDateTime.class))).thenReturn(List.of(currentTxn, pastTxn1, pastTxn2));

    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn(
        "AI Analysis: Appears suspicious due to sudden spike.");

    when(anomalyReportRepository.save(any(AnomalyReport.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    AnomalyReport report = anomalyAnalysisService.analyzeAndReport(currentTxn, ruleResult);

    assertNotNull(report);
    assertEquals("MANUAL_REVIEW", report.getRecommendation());
    assertEquals(40, report.getRiskScore());
    assertEquals("High amount | AI Analysis: AI Analysis: Appears suspicious due to sudden spike.",
        report.getReasoning());
  }

  @Test
  @DisplayName("analyzeAndReport when ChatClient throws exception falls back to rule result")
  void analyzeAndReportLlmErrorFallback() {
    AccountHolder source = new AccountHolder();
    source.setId(2L);

    BankTransaction currentTxn = new BankTransaction();
    currentTxn.setId(200L);
    currentTxn.setTransactionReference("TXN-200");
    currentTxn.setSourceAccountHolder(source);
    currentTxn.setAmount(new BigDecimal("50.00"));

    RuleEvaluationResult ruleResult = new RuleEvaluationResult();
    ruleResult.setAnomalyFlag(AnomalyFlag.NONE);

    when(bankTransactionRepository.findBySourceAccountHolderIdAndCreatedAtAfterOrderByCreatedAtDesc(
        eq(2L), any(OffsetDateTime.class))).thenReturn(List.of());

    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenThrow(new RuntimeException("LLM Timeout"));

    when(anomalyReportRepository.save(any(AnomalyReport.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    AnomalyReport report = anomalyAnalysisService.analyzeAndReport(currentTxn, ruleResult);

    assertNotNull(report);
    assertEquals("APPROVE", report.getRecommendation());
    assertEquals(0, report.getRiskScore());
  }

  @Test
  @DisplayName("analyzeAndReport for deposit transaction (no source account)")
  void analyzeAndReportDepositNoSource() {
    BankTransaction depositTxn = new BankTransaction();
    depositTxn.setId(300L);
    depositTxn.setTransactionReference("TXN-300");
    depositTxn.setType(TransactionType.DEPOSIT);
    depositTxn.setAmount(new BigDecimal("1000.00"));

    RuleEvaluationResult ruleResult = new RuleEvaluationResult();
    ruleResult.setAnomalyFlag(AnomalyFlag.NONE);

    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.user(anyString())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(callResponseSpec);
    when(callResponseSpec.content()).thenReturn(null);

    when(anomalyReportRepository.save(any(AnomalyReport.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    AnomalyReport report = anomalyAnalysisService.analyzeAndReport(depositTxn, ruleResult);

    assertNotNull(report);
    assertEquals("APPROVE", report.getRecommendation());
  }

  @Test
  @DisplayName("getAnomalyReportByTransactionId returns report when found")
  void getAnomalyReportByTransactionIdSuccess() {
    AnomalyReport report = new AnomalyReport();
    report.setId(1L);

    when(anomalyReportRepository.findByTransactionId(100L)).thenReturn(Optional.of(report));

    AnomalyReport result = anomalyAnalysisService.getAnomalyReportByTransactionId(100L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
  }

  @Test
  @DisplayName("getAnomalyReportByTransactionId throws ResourceNotFoundException when missing")
  void getAnomalyReportByTransactionIdNotFound() {
    when(anomalyReportRepository.findByTransactionId(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> anomalyAnalysisService.getAnomalyReportByTransactionId(999L));
  }
}
