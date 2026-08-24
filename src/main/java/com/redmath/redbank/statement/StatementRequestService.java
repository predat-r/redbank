package com.redmath.redbank.statement;

import com.redmath.redbank.statement.dto.StatementRequest;
import com.redmath.redbank.statement.dto.StatementResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class StatementRequestService {

  private final ApplicationEventPublisher eventPublisher;

  public StatementRequestService(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public StatementResponse requestStatement(StatementRequest request, Long accountHolderId) {
    if (request.getFromDate() == null || request.getToDate() == null) {
      throw new IllegalArgumentException("fromDate and toDate are required.");
    }

    if (request.getFromDate().isAfter(request.getToDate())) {
      throw new IllegalArgumentException("fromDate cannot be after toDate.");
    }

    if (request.getToDate().isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("toDate cannot be in the future.");
    }

    long daysBetween = ChronoUnit.DAYS.between(request.getFromDate(), request.getToDate());
    if (daysBetween > 366) {
      throw new IllegalArgumentException("Date range cannot exceed 366 days.");
    }

    eventPublisher.publishEvent(
        new StatementRequestedEvent(accountHolderId, request.getFromDate(), request.getToDate()));

    return new StatementResponse("Your statement will be sent to your registered email shortly.");
  }
}
