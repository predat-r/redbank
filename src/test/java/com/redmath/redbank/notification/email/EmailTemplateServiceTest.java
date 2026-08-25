package com.redmath.redbank.notification.email;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmailTemplateServiceTest {

  private EmailTemplateService emailTemplateService;

  @BeforeEach
  void setUp() {
    emailTemplateService = new EmailTemplateService();
  }

  @Test
  void buildTransferCompletedSenderHtml_containsLogoCidAndDetails() {
    String html = emailTemplateService.buildTransferCompletedSenderHtml(
        "John Doe", new BigDecimal("250.00"), "ACC123", "REF-001", OffsetDateTime.now());

    assertNotNull(html);
    assertTrue(html.contains("<img src=\"cid:logo\""), "HTML should contain inline logo reference");
    assertTrue(html.contains("alt=\"RedBank Logo\""), "HTML should contain logo alt text");
    assertTrue(html.contains("John Doe"));
    assertTrue(html.contains("$250.00"));
    assertTrue(html.contains("ACC123"));
  }

  @Test
  void buildTransferCompletedReceiverHtml_containsLogoCid() {
    String html = emailTemplateService.buildTransferCompletedReceiverHtml(
        "Jane Doe", new BigDecimal("100.00"), "ACC456", "REF-002", OffsetDateTime.now());

    assertNotNull(html);
    assertTrue(html.contains("<img src=\"cid:logo\""));
    assertTrue(html.contains("Jane Doe"));
    assertTrue(html.contains("ACC456"));
  }

  @Test
  void buildTransferCancelledSenderHtml_containsLogoCid() {
    String html = emailTemplateService.buildTransferCancelledSenderHtml(
        "John Doe", new BigDecimal("50.00"), "ACC789", "REF-003", "Insufficient funds", OffsetDateTime.now());

    assertNotNull(html);
    assertTrue(html.contains("<img src=\"cid:logo\""));
    assertTrue(html.contains("Insufficient funds"));
  }

  @Test
  void buildDepositCompletedHtml_containsLogoCid() {
    String html = emailTemplateService.buildDepositCompletedHtml(
        "Alice Smith", new BigDecimal("500.00"), "ACC999", "REF-004", OffsetDateTime.now());

    assertNotNull(html);
    assertTrue(html.contains("<img src=\"cid:logo\""));
    assertTrue(html.contains("Alice Smith"));
    assertTrue(html.contains("$500.00"));
  }

  @Test
  void buildWithdrawalCompletedHtml_containsLogoCid() {
    String html = emailTemplateService.buildWithdrawalCompletedHtml(
        "Bob Johnson", new BigDecimal("75.00"), "ACC888", "REF-005", OffsetDateTime.now());

    assertNotNull(html);
    assertTrue(html.contains("<img src=\"cid:logo\""));
    assertTrue(html.contains("Bob Johnson"));
  }
}
