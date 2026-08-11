package com.redmath.redbank.locationrisk.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class LoginEventServiceTest {

  @Mock
  private LoginEventRepository loginEventRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  private LoginEventService loginEventService;

  @BeforeEach
  void setUp() {
    loginEventService = new LoginEventService(loginEventRepository, eventPublisher);
  }

  @Test
  void recordFailedLoginPersistsFailedEventForKnownUser() {
    LoginContext context = new LoginContext(
        "203.0.113.10",
        "TestBrowser/1.0",
        null
    );

    when(loginEventRepository.save(any(LoginEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoginEvent result = loginEventService.recordFailedLogin(
        42L,
        context,
        "INVALID_PASSWORD"
    );

    ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
    verify(loginEventRepository).save(captor.capture());
    LoginEvent savedEvent = captor.getValue();

    assertNotNull(result);
    assertEquals(42L, savedEvent.getUserId());
    assertEquals("203.0.113.10", savedEvent.getIpAddress());
    assertEquals("TestBrowser/1.0", savedEvent.getUserAgent());
    assertEquals(false, savedEvent.getSuccessful());
    assertEquals("INVALID_PASSWORD", savedEvent.getFailureReason());
    assertNull(savedEvent.getAccessTokenJti());
    assertNotNull(savedEvent.getOccurredAt());
}

  @Test
  void recordSuccessfulLoginPersistsSuccessfulEventWithAccessTokenJti() {
    LoginContext context = new LoginContext(
        "203.0.113.20",
        "TestBrowser/2.0",
        null
    );

    when(loginEventRepository.save(any(LoginEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LoginEvent result = loginEventService.recordSuccessfulLogin(
        84L,
        context,
        "access-token-jti-123",
        Instant.now().plusSeconds(900)
    );

    ArgumentCaptor<LoginEvent> captor = ArgumentCaptor.forClass(LoginEvent.class);
    verify(loginEventRepository).save(captor.capture());
    LoginEvent savedEvent = captor.getValue();

    assertNotNull(result);
    assertEquals(84L, savedEvent.getUserId());
    assertEquals("203.0.113.20", savedEvent.getIpAddress());
    assertEquals("TestBrowser/2.0", savedEvent.getUserAgent());
    assertEquals(true, savedEvent.getSuccessful());
    assertNull(savedEvent.getFailureReason());
    assertEquals("access-token-jti-123", savedEvent.getAccessTokenJti());
    assertNotNull(savedEvent.getOccurredAt());
}

  @Test
  void updateLocationUpdatesExistingLoginEvent() {
    LoginEvent event = new LoginEvent();
    event.setId(99L);
    event.setCity(null);
    event.setCountry(null);
    when(loginEventRepository.findById(99L)).thenReturn(Optional.of(event));

    loginEventService.updateLocation(99L, "Karachi", "Pakistan");

    assertEquals("Karachi", event.getCity());
    assertEquals("Pakistan", event.getCountry());
  }
}
