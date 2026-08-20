package com.redmath.redbank.statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class StatementGenerationListener {

  private static final Logger log = LoggerFactory.getLogger(StatementGenerationListener.class);

  private final StatementProcessorService statementProcessorService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public StatementGenerationListener(StatementProcessorService statementProcessorService) {
    this.statementProcessorService = statementProcessorService;
  }

  @Async("statementTaskExecutor")
  @EventListener
  public void handleStatementRequested(StatementRequestedEvent event) {
    try {
      statementProcessorService.processStatementRequest(event);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Failed to process StatementRequestedEvent for account holder {}",
            event.accountHolderId(), e);
      }
    }
  }
}
