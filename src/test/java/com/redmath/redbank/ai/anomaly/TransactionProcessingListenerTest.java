package com.redmath.redbank.ai.anomaly;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.anomaly.AnomalyAnalysisService;
import com.redmath.redbank.anomaly.AnomalyDetectionService;
import com.redmath.redbank.anomaly.RuleEvaluationResult;
import com.redmath.redbank.anomaly.TransactionProcessingListener;
import com.redmath.redbank.transaction.AnomalyFlag;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.event.TransactionSubmittedEvent;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionProcessingListenerTest {

  @Mock
  private BankTransactionRepository bankTransactionRepository;

  @Mock
  private BankTransactionService bankTransactionService;

  @Mock
  private AnomalyDetectionService anomalyDetectionService;

  @Mock
  private AnomalyAnalysisService anomalyAnalysisService;

  private TransactionProcessingListener listener;

  @BeforeEach
  void setUp() {
    listener = new TransactionProcessingListener(
        bankTransactionRepository,
        bankTransactionService,
        anomalyDetectionService,
        anomalyAnalysisService);
  }

  @Test
  @DisplayName("handleTransactionSubmitted does nothing when event or transactionId is null")
  void handleTransactionSubmittedNullEvent() {
    listener.handleTransactionSubmitted(null);
    listener.handleTransactionSubmitted(new TransactionSubmittedEvent(null));

    verify(bankTransactionRepository, never()).findById(any());
  }

  @Test
  @DisplayName("handleTransactionSubmitted does nothing when transaction is not found")
  void handleTransactionSubmittedNotFound() {
    when(bankTransactionRepository.findById(99L)).thenReturn(Optional.empty());

    listener.handleTransactionSubmitted(new TransactionSubmittedEvent(99L));

    verify(anomalyDetectionService, never()).evaluate(any());
  }

  @Test
  @DisplayName("handleTransactionSubmitted completes transaction when risk score is clean (<30)")
  void handleTransactionSubmittedClean() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(10L);
    transaction.setTransactionReference("TXN-CLEAN-001");

    RuleEvaluationResult ruleResult = new RuleEvaluationResult();
    ruleResult.setAnomalyFlag(AnomalyFlag.NONE);

    when(bankTransactionRepository.findById(10L)).thenReturn(Optional.of(transaction));
    when(anomalyDetectionService.evaluate(transaction)).thenReturn(ruleResult);

    listener.handleTransactionSubmitted(new TransactionSubmittedEvent(10L));

    verify(bankTransactionRepository).save(transaction);
    verify(bankTransactionService).completePendingTransaction(10L);
    verify(anomalyAnalysisService, never()).analyzeAndReport(any(), any());
  }

  @Test
  @DisplayName("handleTransactionSubmitted invokes AI analysis when risk score is suspicious (>=30)")
  void handleTransactionSubmittedSuspicious() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(20L);
    transaction.setTransactionReference("TXN-SUSP-001");

    RuleEvaluationResult ruleResult = new RuleEvaluationResult();
    ruleResult.addReason("High velocity", 35);
    ruleResult.setAnomalyFlag(AnomalyFlag.MEDIUM);

    when(bankTransactionRepository.findById(20L)).thenReturn(Optional.of(transaction));
    when(anomalyDetectionService.evaluate(transaction)).thenReturn(ruleResult);

    listener.handleTransactionSubmitted(new TransactionSubmittedEvent(20L));

    verify(bankTransactionRepository).save(transaction);
    verify(bankTransactionService, never()).completePendingTransaction(any());
    verify(anomalyAnalysisService).analyzeAndReport(transaction, ruleResult);
  }

  @Test
  @DisplayName("handleTransactionSubmitted safely catches exceptions")
  void handleTransactionSubmittedExceptionCaught() {
    when(bankTransactionRepository.findById(30L)).thenThrow(new RuntimeException("Database error"));

    listener.handleTransactionSubmitted(new TransactionSubmittedEvent(30L));

    verify(anomalyDetectionService, never()).evaluate(any());
  }
}
