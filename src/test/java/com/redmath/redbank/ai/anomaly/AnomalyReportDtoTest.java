package com.redmath.redbank.ai.anomaly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.redmath.redbank.transaction.BankTransaction;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnomalyReportDtoTest {

  @Test
  @DisplayName("from returns null when report is null")
  void fromNullReport() {
    assertNull(AnomalyReportDto.from(null));
  }

  @Test
  @DisplayName("from maps all fields correctly")
  void fromValidReport() {
    BankTransaction transaction = new BankTransaction();
    transaction.setId(10L);
    transaction.setTransactionReference("TXN-100");

    AnomalyReport report = new AnomalyReport();
    report.setId(1L);
    report.setTransaction(transaction);
    report.setRiskScore(75);
    report.setRecommendation("MANUAL_REVIEW");
    report.setReasoning("High amount");
    report.setCreatedAt(OffsetDateTime.now());

    AnomalyReportDto dto = AnomalyReportDto.from(report);

    assertNotNull(dto);
    assertEquals(1L, dto.getId());
    assertEquals(10L, dto.getTransactionId());
    assertEquals("TXN-100", dto.getTransactionReference());
    assertEquals(75, dto.getRiskScore());
    assertEquals("MANUAL_REVIEW", dto.getRecommendation());
    assertEquals("High amount", dto.getReasoning());
    assertEquals(report.getCreatedAt(), dto.getCreatedAt());
  }
}
