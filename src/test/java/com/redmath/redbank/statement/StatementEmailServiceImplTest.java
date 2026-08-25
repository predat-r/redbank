package com.redmath.redbank.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.notification.email.EmailMessage;
import com.redmath.redbank.notification.email.EmailService;
import com.redmath.redbank.notification.email.EmailTemplateService;
import com.redmath.redbank.user.User;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatementEmailServiceImplTest {

  @Mock
  private EmailService emailService;

  @Mock
  private EmailTemplateService emailTemplateService;

  private StatementEmailServiceImpl statementEmailService;

  @BeforeEach
  void setUp() {
    statementEmailService = new StatementEmailServiceImpl(emailService, emailTemplateService);
  }

  @Test
  void sendStatement_validAccountHolder_sendsEmailWithPdfAttachment() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setAccountNumber("RB1000000001");
    accountHolder.setUser(User.builder().name("John Doe").email("john@example.com").build());

    byte[] pdfBytes = new byte[]{10, 20, 30};
    LocalDate fromDate = LocalDate.of(2026, 1, 1);
    LocalDate toDate = LocalDate.of(2026, 1, 31);

    when(emailTemplateService.buildStatementCompletedHtml("John Doe", "RB1000000001", fromDate, toDate))
        .thenReturn("<html>Statement Body</html>");
    when(emailService.sendEmailWithAttachment(any(EmailMessage.class), any(), any(), any()))
        .thenReturn(true);

    statementEmailService.sendStatement(accountHolder, pdfBytes, fromDate, toDate);

    ArgumentCaptor<EmailMessage> messageCaptor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailService).sendEmailWithAttachment(
        messageCaptor.capture(),
        eq("Statement_RB1000000001_2026-01-01_to_2026-01-31.pdf"),
        eq(pdfBytes),
        eq("application/pdf")
    );

    EmailMessage message = messageCaptor.getValue();
    assertEquals("john@example.com", message.getTo());
    assertTrue(message.getSubject().contains("RedBank: Account Statement"));
    assertEquals("<html>Statement Body</html>", message.getBody());
    assertTrue(message.isHtml());
  }

  @Test
  void sendStatement_missingEmail_skipsSending() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setUser(User.builder().name("No Email").email("").build());

    statementEmailService.sendStatement(accountHolder, new byte[]{1}, LocalDate.now(), LocalDate.now());

    verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any());
  }

  @Test
  void sendStatement_nullAccountHolder_skipsSending() {
    statementEmailService.sendStatement(null, new byte[]{1}, LocalDate.now(), LocalDate.now());

    verify(emailService, never()).sendEmailWithAttachment(any(), any(), any(), any());
  }
}
