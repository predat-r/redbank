package com.redmath.redbank.statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.redmath.redbank.statement.dto.StatementData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatementPdfGeneratorTest {

  private StatementPdfGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new StatementPdfGenerator("static/branding/logo.png");
  }

  @Test
  void shouldGeneratePdf() {
    StatementData data = StatementData.builder()
        .accountHolderName("John Doe")
        .accountNumber("123456")
        .address("123 Main St")
        .currency("USD")
        .fromDate(LocalDate.now().minusDays(30))
        .toDate(LocalDate.now())
        .openingBalance(BigDecimal.valueOf(100.00))
        .closingBalance(BigDecimal.valueOf(200.00))
        .totalCredits(BigDecimal.valueOf(100.00))
        .totalDebits(BigDecimal.ZERO)
        .transactionCount(0)
        .generationTimestamp(OffsetDateTime.now())
        .transactions(Collections.emptyList())
        .build();

    byte[] pdfBytes = generator.generatePdf(data);
    
    assertNotNull(pdfBytes);
    assertTrue(pdfBytes.length > 0);
  }
}
