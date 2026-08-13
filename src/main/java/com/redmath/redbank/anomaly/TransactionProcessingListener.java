package com.redmath.redbank.anomaly;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.BankTransactionService;
import com.redmath.redbank.transaction.event.TransactionSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class TransactionProcessingListener {

  private static final Logger log = LoggerFactory.getLogger(TransactionProcessingListener.class);

  private final BankTransactionRepository bankTransactionRepository;
  private final BankTransactionService bankTransactionService;
  private final AnomalyDetectionService anomalyDetectionService;
  private final AnomalyAnalysisService anomalyAnalysisService;

  public TransactionProcessingListener(
      BankTransactionRepository bankTransactionRepository,
      BankTransactionService bankTransactionService,
      AnomalyDetectionService anomalyDetectionService,
      AnomalyAnalysisService anomalyAnalysisService) {
    this.bankTransactionRepository = bankTransactionRepository;
    this.bankTransactionService = bankTransactionService;
    this.anomalyDetectionService = anomalyDetectionService;
    this.anomalyAnalysisService = anomalyAnalysisService;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleTransactionSubmitted(TransactionSubmittedEvent event) {
    if (event == null || event.getTransactionId() == null) {
      return;
    }

    try {
      BankTransaction transaction = bankTransactionRepository.findById(event.getTransactionId())
          .orElse(null);
      if (transaction == null) {
        return;
      }

      RuleEvaluationResult ruleResult = anomalyDetectionService.evaluate(transaction);
      transaction.setAnomalyFlag(ruleResult.getAnomalyFlag());
      bankTransactionRepository.save(transaction);

      if (ruleResult.getRiskScore() < 30) {
        if (log.isInfoEnabled()) {
          log.info("Transaction {} evaluated as CLEAN (riskScore: {}). Auto-completing.",
              transaction.getTransactionReference(), ruleResult.getRiskScore());
        }
        bankTransactionService.completePendingTransaction(transaction.getId());
      } else {
        if (log.isWarnEnabled()) {
          log.warn(
              "Transaction {} flagged as SUSPICIOUS (anomalyFlag: {}, riskScore: {}). Leaving PENDING for admin approval.",
              transaction.getTransactionReference(), ruleResult.getAnomalyFlag(),
              ruleResult.getRiskScore());
        }
        anomalyAnalysisService.analyzeAndReport(transaction, ruleResult);
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error during async processing of transaction ID {}: {}",
            event.getTransactionId(), e.getMessage(), e);
      }
    }
  }
}
