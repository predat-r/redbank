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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenFromDateIsAfterToDate() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now());
    request.setToDate(LocalDate.now().minusDays(1));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenToDateIsInFuture() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now());
    request.setToDate(LocalDate.now().plusDays(1));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  void shouldThrowWhenDateRangeExceeds366Days() {
    StatementRequest request = new StatementRequest();
    request.setFromDate(LocalDate.now().minusDays(400));
    request.setToDate(LocalDate.now());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> statementRequestService.requestStatement(request, 10L));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }
}
