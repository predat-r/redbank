package com.redmath.redbank.ai.anomaly;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnomalyReportDto {

  private Long id;
  private Long transactionId;
  private String transactionReference;
  private Integer riskScore;
  private String recommendation;
  private String reasoning;
  private OffsetDateTime createdAt;

  public static AnomalyReportDto from(AnomalyReport report) {
    if (report == null) {
      return null;
    }
    AnomalyReportDto dto = new AnomalyReportDto();
    dto.setId(report.getId());
    if (report.getTransaction() != null) {
      dto.setTransactionId(report.getTransaction().getId());
      dto.setTransactionReference(report.getTransaction().getTransactionReference());
    }
    dto.setRiskScore(report.getRiskScore());
    dto.setRecommendation(report.getRecommendation());
    dto.setReasoning(report.getReasoning());
    dto.setCreatedAt(report.getCreatedAt());
    return dto;
  }
}
