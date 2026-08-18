package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.notification.email.EmailMessage;
import com.redmath.redbank.notification.email.EmailService;
import com.redmath.redbank.notification.email.EmailTemplateService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementEmailServiceImpl implements StatementEmailService {

  private final EmailService emailService;
  private final EmailTemplateService emailTemplateService;

  @Override
  public void sendStatement(AccountHolder accountHolder, byte[] pdfBytes, LocalDate fromDate, LocalDate toDate) {
    if (accountHolder == null || accountHolder.getUser() == null || !StringUtils.hasText(accountHolder.getUser().getEmail())) {
      log.warn("Cannot send statement email: account holder or recipient email is missing");
      return;
    }

    String recipientEmail = accountHolder.getUser().getEmail();
    String recipientName = accountHolder.getUser().getName();
    String accountNumber = accountHolder.getAccountNumber();

    String htmlBody = emailTemplateService.buildStatementCompletedHtml(recipientName, accountNumber, fromDate, toDate);
    String subject = String.format("RedBank: Account Statement (%s to %s)", fromDate, toDate);
    String filename = String.format("Statement_%s_%s_to_%s.pdf", accountNumber, fromDate, toDate);

    boolean sent = emailService.sendEmailWithAttachment(
        EmailMessage.builder()
            .to(recipientEmail)
            .subject(subject)
            .body(htmlBody)
            .isHtml(true)
            .build(),
        filename,
        pdfBytes,
        "application/pdf"
    );

    if (sent) {
      log.info("Statement email successfully delivered to {}", recipientEmail);
    } else {
      log.info("Statement email delivery skipped or suppressed for {}", recipientEmail);
    }
  }
}
