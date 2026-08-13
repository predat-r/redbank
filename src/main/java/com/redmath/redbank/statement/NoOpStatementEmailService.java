package com.redmath.redbank.statement;

import com.redmath.redbank.account.AccountHolder;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NoOpStatementEmailService implements StatementEmailService {

  private static final Logger log = LoggerFactory.getLogger(NoOpStatementEmailService.class);

  @Override
  public void sendStatement(AccountHolder accountHolder, byte[] pdfBytes, LocalDate fromDate, LocalDate toDate) {
    if (log.isInfoEnabled()) {
      log.info("Statement generated and ready to email for account number {}, date range: {} to {}, size: {} bytes",
          accountHolder.getAccountNumber(), fromDate, toDate, pdfBytes != null ? pdfBytes.length : 0);
    }
  }
}
