package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import java.time.LocalDate;

@FunctionalInterface
public interface StatementEmailService {
  // TODO(email): implement actual email delivery (e.g. via Spring Mail / a transactional email provider).
  // Should send `pdfBytes` as an attachment to the account holder's registered email address,
  // with a subject/body appropriate for a bank statement notification.
  void sendStatement(AccountHolder accountHolder, byte[] pdfBytes, LocalDate fromDate, LocalDate toDate);
}
