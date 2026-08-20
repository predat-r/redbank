package com.redmath.redbank.ai.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.anomaly.AnomalyDetectionService;
import com.redmath.redbank.transaction.AnomalyFlag;
import com.redmath.redbank.anomaly.RuleEvaluationResult;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

  @Mock
  private BankTransactionRepository bankTransactionRepository;

  private AnomalyDetectionService anomalyDetectionService;

  @BeforeEach
  void setUp() {
    anomalyDetectionService = new AnomalyDetectionService(bankTransactionRepository);
  }

  @Test
  @DisplayName("evaluate returns empty result when transaction is null")
  void evaluateNullTransaction() {
    RuleEvaluationResult result = anomalyDetectionService.evaluate(null);
    assertNotNull(result);
    assertEquals(0, result.getRiskScore());
    assertEquals(AnomalyFlag.NONE, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags normal transaction as NONE")
  void evaluateNormalTransaction() {
    BankTransaction transaction = new BankTransaction();
    transaction.setAmount(new BigDecimal("50.00"));
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 14, 0, 0, 0, ZoneOffset.UTC));

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(0, result.getRiskScore());
    assertEquals(AnomalyFlag.NONE, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags high amount as MEDIUM")
  void evaluateHighAmount() {
    BankTransaction transaction = new BankTransaction();
    transaction.setAmount(new BigDecimal("15000.00"));
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 14, 0, 0, 0, ZoneOffset.UTC));

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(40, result.getRiskScore());
    assertEquals(AnomalyFlag.MEDIUM, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags extremely high amount as HIGH")
  void evaluateVeryHighAmount() {
    BankTransaction transaction = new BankTransaction();
    transaction.setAmount(new BigDecimal("60000.00"));
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 14, 0, 0, 0, ZoneOffset.UTC));

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(70, result.getRiskScore());
    assertEquals(AnomalyFlag.HIGH, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags off-peak hours as LOW")
  void evaluateOffPeakHours() {
    BankTransaction transaction = new BankTransaction();
    transaction.setAmount(new BigDecimal("20.00"));
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 3, 0, 0, 0, ZoneOffset.UTC));

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(25, result.getRiskScore());
    assertEquals(AnomalyFlag.LOW, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags high velocity as MEDIUM")
  void evaluateHighVelocity() {
    AccountHolder source = new AccountHolder();
    source.setId(1L);

    BankTransaction transaction = new BankTransaction();
    transaction.setSourceAccountHolder(source);
    transaction.setAmount(new BigDecimal("20.00"));
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 14, 0, 0, 0, ZoneOffset.UTC));

    when(bankTransactionRepository.countBySourceAccountHolderIdAndCreatedAtAfter(eq(1L),
        any(OffsetDateTime.class)))
        .thenReturn(12L);

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(35, result.getRiskScore());
    assertEquals(AnomalyFlag.MEDIUM, result.getAnomalyFlag());
  }

  @Test
  @DisplayName("evaluate flags combined rules as CRITICAL")
  void evaluateCombinedCritical() {
    AccountHolder source = new AccountHolder();
    source.setId(1L);

    BankTransaction transaction = new BankTransaction();
    transaction.setSourceAccountHolder(source);
    transaction.setAmount(new BigDecimal("60000.00")); // 70
    transaction.setCreatedAt(OffsetDateTime.of(2026, 8, 11, 2, 0, 0, 0, ZoneOffset.UTC)); // 25

    when(bankTransactionRepository.countBySourceAccountHolderIdAndCreatedAtAfter(eq(1L),
        any(OffsetDateTime.class)))
        .thenReturn(15L); // 35

    RuleEvaluationResult result = anomalyDetectionService.evaluate(transaction);

    assertEquals(130, result.getRiskScore());
    assertEquals(AnomalyFlag.CRITICAL, result.getAnomalyFlag());
  }
}
