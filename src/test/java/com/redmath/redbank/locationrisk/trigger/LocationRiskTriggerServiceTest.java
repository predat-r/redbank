package com.redmath.redbank.locationrisk.trigger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationProvider;
import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.login.LoginEvent;
import com.redmath.redbank.locationrisk.loginhistory.LoginHistoryService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationRiskTriggerServiceTest {

  @Mock
  private IpGeolocationProvider geolocationProvider;

  @Mock
  private LoginHistoryService loginHistoryService;

  private LocationRiskTriggerService service;

  @BeforeEach
  void setUp() {
    service = new LocationRiskTriggerService(geolocationProvider, loginHistoryService);
  }

  @Test
  void doesNotRequireAssessmentWhenGeolocationFails() {
    when(geolocationProvider.resolve("198.51.100.10"))
        .thenReturn(result("198.51.100.10", null, null, false));

    LocationRiskTriggerResult result = service.assessLocationRisk(1L, "198.51.100.10", 10L);

    assertFalse(result.requiresAiAssessment());
    verifyNoInteractions(loginHistoryService);
  }

  @Test
  void doesNotRequireAssessmentForFirstSuccessfulLogin() {
    when(geolocationProvider.resolve("198.51.100.10"))
        .thenReturn(result("198.51.100.10", "Lahore", "Pakistan", true));
    when(loginHistoryService.getLatestSuccessfulLoginExcluding(1L, 10L))
        .thenReturn(Optional.empty());

    LocationRiskTriggerResult trigger = service.assessLocationRisk(1L, "198.51.100.10", 10L);

    assertFalse(trigger.requiresAiAssessment());
  }

  @Test
  void doesNotRequireAssessmentWhenIpWasPreviouslyUsed() {
    when(geolocationProvider.resolve("198.51.100.10"))
        .thenReturn(result("198.51.100.10", "Lahore", "Pakistan", true));
    when(loginHistoryService.getLatestSuccessfulLoginExcluding(1L, 10L))
        .thenReturn(Optional.of(login("Lahore")));
    when(loginHistoryService.hasUsedIpBeforeExcluding(1L, "198.51.100.10", 10L))
        .thenReturn(true);

    LocationRiskTriggerResult trigger = service.assessLocationRisk(1L, "198.51.100.10", 10L);

    assertFalse(trigger.requiresAiAssessment());
    assertTrue(trigger.ipPreviouslyUsed());
  }

  @Test
  void doesNotRequireAssessmentWhenCityIsUnchanged() {
    when(geolocationProvider.resolve("198.51.100.10"))
        .thenReturn(result("198.51.100.10", "Lahore", "Pakistan", true));
    when(loginHistoryService.getLatestSuccessfulLoginExcluding(1L, 10L))
        .thenReturn(Optional.of(login("Lahore")));
    when(loginHistoryService.hasUsedIpBeforeExcluding(1L, "198.51.100.10", 10L))
        .thenReturn(false);

    LocationRiskTriggerResult trigger = service.assessLocationRisk(1L, "198.51.100.10", 10L);

    assertFalse(trigger.requiresAiAssessment());
  }

  @Test
  void requiresAssessmentForNewIpFromDifferentCity() {
    when(geolocationProvider.resolve("198.51.100.10"))
        .thenReturn(result("198.51.100.10", "Karachi", "Pakistan", true));
    when(loginHistoryService.getLatestSuccessfulLoginExcluding(1L, 10L))
        .thenReturn(Optional.of(login("Lahore")));
    when(loginHistoryService.hasUsedIpBeforeExcluding(1L, "198.51.100.10", 10L))
        .thenReturn(false);

    LocationRiskTriggerResult trigger = service.assessLocationRisk(1L, "198.51.100.10", 10L);

    assertTrue(trigger.requiresAiAssessment());
    assertFalse(trigger.ipPreviouslyUsed());
  }

  private LoginEvent login(String city) {
    LoginEvent event = new LoginEvent();
    event.setCity(city);
    event.setOccurredAt(Instant.now());
    return event;
  }

  private IpGeolocationResult result(String ip, String city, String country, boolean successful) {
    return new IpGeolocationResult(ip, city, country, "PK", "test", successful);
  }
}
