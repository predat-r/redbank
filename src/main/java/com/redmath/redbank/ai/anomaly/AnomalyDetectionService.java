package com.redmath.redbank.ai.anomaly;

import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class AnomalyDetectionService {

  private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
  private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000.00");

  private static final int OFF_PEAK_START_HOUR = 1;
  private static final int OFF_PEAK_END_HOUR = 5;

  private static final int VELOCITY_WINDOW_MINUTES = 5;
  private static final int VELOCITY_THRESHOLD = 10;

  private static final int SCORE_VERY_HIGH_AMOUNT = 70;
  private static final int SCORE_HIGH_AMOUNT = 40;
  private static final int SCORE_OFF_PEAK = 25;
  private static final int SCORE_VELOCITY = 35;

  private static final int THRESHOLD_CRITICAL_SCORE = 75;
  private static final int THRESHOLD_HIGH_SCORE = 50;
  private static final int THRESHOLD_MEDIUM_SCORE = 30;
  private static final int THRESHOLD_LOW_SCORE = 15;

  private final BankTransactionRepository bankTransactionRepository;

  public AnomalyDetectionService(BankTransactionRepository bankTransactionRepository) {
    this.bankTransactionRepository = bankTransactionRepository;
  }

  public RuleEvaluationResult evaluate(BankTransaction transaction) {
    RuleEvaluationResult result = new RuleEvaluationResult();

    if (transaction == null) {
      return result;
    }

    // 1. Amount Threshold Check
    BigDecimal amount = transaction.getAmount();
    if (amount != null) {
      if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
        result.addReason("Extremely high transaction amount ($" + amount + ")",
            SCORE_VERY_HIGH_AMOUNT);
      } else if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
        result.addReason("High transaction amount ($" + amount + ")", SCORE_HIGH_AMOUNT);
      }
    }

    // 2. Unusual Hours Check
    OffsetDateTime createdAt = transaction.getCreatedAt() != null
        ? transaction.getCreatedAt()
        : OffsetDateTime.now(ZoneOffset.UTC);
    int hour = createdAt.getHour();
    if (hour >= OFF_PEAK_START_HOUR && hour <= OFF_PEAK_END_HOUR) {
      result.addReason("Transaction initiated during unusual off-peak hours (" + hour + ":00 UTC)",
          SCORE_OFF_PEAK);
    }

    // 3. Velocity Check
    if (transaction.getSourceAccountHolder() != null) {
      Long sourceId = transaction.getSourceAccountHolder().getId();
      OffsetDateTime windowStartTime = createdAt.minusMinutes(VELOCITY_WINDOW_MINUTES);
      long recentCount = bankTransactionRepository.countBySourceAccountHolderIdAndCreatedAtAfter(
          sourceId, windowStartTime);
      if (recentCount >= VELOCITY_THRESHOLD) {
        result.addReason("High transaction velocity: " + recentCount + " transactions in past "
            + VELOCITY_WINDOW_MINUTES + " minutes", SCORE_VELOCITY);
      }
    }

    // Map total risk score to AnomalyFlag
    int score = result.getRiskScore();
    if (score >= THRESHOLD_CRITICAL_SCORE) {
      result.setAnomalyFlag(AnomalyFlag.CRITICAL);
    } else if (score >= THRESHOLD_HIGH_SCORE) {
      result.setAnomalyFlag(AnomalyFlag.HIGH);
    } else if (score >= THRESHOLD_MEDIUM_SCORE) {
      result.setAnomalyFlag(AnomalyFlag.MEDIUM);
    } else if (score >= THRESHOLD_LOW_SCORE) {
      result.setAnomalyFlag(AnomalyFlag.LOW);
    } else {
      result.setAnomalyFlag(AnomalyFlag.NONE);
    }

    return result;
  }
}
