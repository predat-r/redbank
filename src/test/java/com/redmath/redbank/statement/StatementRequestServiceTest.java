package com.redmath.redbank.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.redmath.redbank.statement.dto.StatementRequest;
import com.redmath.redbank.statement.dto.StatementResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StatementRequestServiceTest {

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private StatementRequestService statementRequestService;

  @BeforeEach
  void setUp() {
    statementRequestService = new StatementRequestService(eventPublisher);
  }

  @Test
  void shouldPublishEventAndReturnResponse() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now().minusDays(30));
    request.setToDate(LocalDate.now());

    StatementResponse response = statementRequestService.requestStatement(request, 10L);

    assertEquals("Your statement will be sent to your registered email shortly.", response.getMessage());

    ArgumentCaptor<StatementRequestedEvent> captor = ArgumentCaptor.forClass(StatementRequestedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());

    StatementRequestedEvent event = captor.getValue();
    assertEquals(10L, event.accountHolderId());
    assertEquals(request.getFromDate(), event.fromDate());
    assertEquals(request.getToDate(), event.toDate());
  }

  @Test
  void shouldThrowWhenDatesAreNull() {
    StatementRequest request = new StatementRequest();
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals("fromDate and toDate are required.", ex.getMessage());
  }

  @Test
  void shouldThrowWhenFromDateIsAfterToDate() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now());
    request.setToDate(LocalDate.now().minusDays(1));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals("fromDate cannot be after toDate.", ex.getMessage());
  }

  @Test
  void shouldThrowWhenToDateIsInFuture() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now());
    request.setToDate(LocalDate.now().plusDays(1));

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals("toDate cannot be in the future.", ex.getMessage());
  }

  @Test
  void shouldThrowWhenDateRangeExceeds366Days() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now().minusDays(400));
    request.setToDate(LocalDate.now());

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals("Date range cannot exceed 366 days.", ex.getMessage());
  }
}
