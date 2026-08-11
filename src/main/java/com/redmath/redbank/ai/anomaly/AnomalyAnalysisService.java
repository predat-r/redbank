package com.redmath.redbank.ai.anomaly;

import com.redmath.redbank.common.exception.ResourceNotFoundException;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnomalyAnalysisService {

  private static final Logger log = LoggerFactory.getLogger(AnomalyAnalysisService.class);
  private static final int BEHAVIORAL_HISTORY_DAYS = 30;

  private final ChatClient chatClient;
  private final AnomalyReportRepository anomalyReportRepository;
  private final BankTransactionRepository bankTransactionRepository;

  public AnomalyAnalysisService(ChatClient chatClient,
      AnomalyReportRepository anomalyReportRepository,
      BankTransactionRepository bankTransactionRepository) {
    this.chatClient = chatClient;
    this.anomalyReportRepository = anomalyReportRepository;
    this.bankTransactionRepository = bankTransactionRepository;
  }

  @Transactional
  public AnomalyReport analyzeAndReport(BankTransaction transaction,
      RuleEvaluationResult ruleResult) {
    int finalScore = ruleResult.getRiskScore();
    String recommendation = ruleResult.getAnomalyFlag() == AnomalyFlag.HIGH
        || ruleResult.getAnomalyFlag() == AnomalyFlag.CRITICAL
        ? "MANUAL_REVIEW" : "APPROVE";
    String reasoning = String.join("; ", ruleResult.getReasons());

    try {
      String behavioralContext = buildBehavioralContext(transaction);
      String userPrompt = buildPrompt(transaction, ruleResult, reasoning, behavioralContext);

      if (log.isInfoEnabled()) {
        log.info("Initiating Spring AI LLM anomaly analysis for transaction {}",
            transaction.getTransactionReference());
      }

      String aiResponse = chatClient.prompt().user(userPrompt).call().content();

      if (aiResponse != null && !aiResponse.isBlank()) {
        if (log.isInfoEnabled()) {
          log.info("Spring AI LLM anomaly analysis completed successfully for transaction {}",
              transaction.getTransactionReference());
        }
        reasoning = (reasoning.isEmpty() ? "" : reasoning + " | AI Analysis: ")
            + aiResponse.trim();
      } else {
        if (log.isWarnEnabled()) {
          log.warn("Spring AI LLM returned empty response for transaction {}",
              transaction.getTransactionReference());
        }
      }
    } catch (Exception e) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Spring AI LLM call failed for transaction {}: {}. Falling back to rule engine result.",
            transaction.getTransactionReference(), e.getMessage());
      }
    }

    AnomalyReport report = new AnomalyReport();
    report.setTransaction(transaction);
    report.setRiskScore(finalScore);
    report.setRecommendation(recommendation);
    report.setReasoning(reasoning);
    report.setCreatedAt(OffsetDateTime.now());

    return anomalyReportRepository.save(report);
  }

  public AnomalyReport getAnomalyReportByTransactionId(Long transactionId) {
    return anomalyReportRepository.findByTransactionId(transactionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Anomaly report not found for transaction ID: " + transactionId));
  }

  private String buildBehavioralContext(BankTransaction transaction) {
    if (transaction.getSourceAccountHolder() == null) {
      return "No source account history available (deposit transaction).";
    }

    Long sourceId = transaction.getSourceAccountHolder().getId();
    OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC)
        .minusDays(BEHAVIORAL_HISTORY_DAYS);

    List<BankTransaction> recentTransactions =
        bankTransactionRepository.findBySourceAccountHolderIdAndCreatedAtAfterOrderByCreatedAtDesc(
            sourceId, cutoff).stream()
        .filter(t -> !t.getId().equals(transaction.getId()))
        .toList();

    if (recentTransactions.isEmpty()) {
      return "No transaction history found in the last " + BEHAVIORAL_HISTORY_DAYS + " days. "
          + "This may be a new account.";
    }

    BigDecimal totalAmount = recentTransactions.stream()
        .map(BankTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal avgAmount = totalAmount.divide(
        BigDecimal.valueOf(recentTransactions.size()), 2, RoundingMode.HALF_UP);

    BigDecimal maxAmount = recentTransactions.stream()
        .map(BankTransaction::getAmount)
        .max(BigDecimal::compareTo)
        .orElse(BigDecimal.ZERO);

    Map<String, Long> categoryBreakdown = recentTransactions.stream()
        .collect(Collectors.groupingBy(
            t -> t.getCategory() != null ? t.getCategory().name() : "UNCATEGORIZED",
            Collectors.counting()));

    String topCategories = categoryBreakdown.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(3)
        .map(e -> e.getKey() + " (" + e.getValue() + ")")
        .collect(Collectors.joining(", "));

    Map<Integer, Long> hourDistribution = recentTransactions.stream()
        .collect(Collectors.groupingBy(
            t -> t.getCreatedAt().getHour(),
            Collectors.counting()));

    int peakHour = hourDistribution.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .orElse(12);

    double weeklyFrequency = recentTransactions.size()
        / Math.max(1.0, BEHAVIORAL_HISTORY_DAYS / 7.0);

    return String.format(
        "User Behavioral Profile (last %d days):%n"
            + "- Total transactions: %d%n"
            + "- Average transaction amount: $%s%n"
            + "- Largest single transaction: $%s%n"
            + "- Weekly frequency: %.1f transactions/week%n"
            + "- Top categories: %s%n"
            + "- Most active hour: %d:00 UTC",
        BEHAVIORAL_HISTORY_DAYS,
        recentTransactions.size(),
        avgAmount,
        maxAmount,
        weeklyFrequency,
        topCategories,
        peakHour);
  }

  private String buildPrompt(BankTransaction transaction, RuleEvaluationResult ruleResult,
      String reasoning, String behavioralContext) {
    return String.format(
        "You are a financial fraud and transaction anomaly analyst for RedBank.%n%n"
            + "%s%n%n"
            + "Current Transaction Under Review:%n"
            + "- Reference: %s%n"
            + "- Type: %s%n"
            + "- Category: %s%n"
            + "- Amount: $%s%n"
            + "- Rule Risk Score: %d%n"
            + "- Rule Flags: %s%n%n"
            + "Compare the current transaction against the user's established behavioral pattern. "
            + "Provide a brief 2-3 sentence assessment of whether this transaction is suspicious "
            + "or consistent with the user's habits, and give a clear recommendation "
            + "(APPROVE, MANUAL_REVIEW, or REJECT).",
        behavioralContext,
        transaction.getTransactionReference(),
        transaction.getType(),
        transaction.getCategory() != null ? transaction.getCategory() : "UNSPECIFIED",
        transaction.getAmount(),
        ruleResult.getRiskScore(),
        reasoning.isEmpty() ? "None" : reasoning);
  }
}
