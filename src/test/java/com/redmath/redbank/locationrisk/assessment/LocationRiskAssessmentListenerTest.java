package com.redmath.redbank.locationrisk.assessment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.login.LoginEventService;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerResult;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerService;
import com.redmath.redbank.security.denylist.TokenDenylistService;
import com.redmath.redbank.user.UserService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocationRiskAssessmentListenerTest {

  @Mock
  private LocationRiskTriggerService triggerService;

  @Mock
  private LocationRiskAssessmentService assessmentService;

  @Mock
  private AuditService auditService;

  @Mock
  private TokenDenylistService tokenDenylistService;

  @Mock
  private LoginEventService loginEventService;

  @Mock
  private UserService userService;

  private LocationRiskAssessmentListener listener;

  @BeforeEach
  void setUp() {
    listener = new LocationRiskAssessmentListener(
        triggerService, loginEventService, assessmentService, auditService, tokenDenylistService,
        userService);
  }

  @Test
  void ignoresFailedLogin() {
    listener.handle(event(false, Instant.now().plusSeconds(900)));

    verify(triggerService, never()).assessLocationRisk(any(), any(), any());
    verifyNoAssessmentSideEffects();
  }

  @Test
  void stopsWhenTriggerDoesNotRequireAi() {
    LocationRiskAssessmentRequested event = event(true, Instant.now().plusSeconds(900));
    when(triggerService.assessLocationRisk(42L, "198.51.100.10", 99L))
        .thenReturn(new LocationRiskTriggerResult(
            location(), false, false));

    listener.handle(event);

    verify(loginEventService).updateLocation(99L, "Karachi", "Pakistan");
    verify(assessmentService, never()).assess(any(), any(), any(), any());
    verifyNoAssessmentSideEffects();
  }

  @Test
  void auditsAndDenylistsExtremeSessionRevocation() {
    Instant expiresAt = Instant.now().plusSeconds(900);
    LocationRiskAssessmentRequested event = event(true, expiresAt);
    when(triggerService.assessLocationRisk(42L, "198.51.100.10", 99L))
        .thenReturn(new LocationRiskTriggerResult(location(), false, true));
    when(assessmentService.assess(42L, "198.51.100.10", 99L, location()))
        .thenReturn(new LocationRiskAssessment(
            RiskLevel.EXTREME, "New city and malicious IP", AssessmentConfidence.HIGH,
            RecommendedAction.REVOKE_SESSION));

    listener.handle(event);

    verify(loginEventService).updateLocation(99L, "Karachi", "Pakistan");
    verify(tokenDenylistService).deny(any(), any());
    verify(userService).invalidateRefreshTokens(42L);
    verify(auditService).recordAuditLog(any(), any(), any(), any(), any());
  }

  @Test
  void doesNotDenylistNonExtremeAssessment() {
    Instant expiresAt = Instant.now().plusSeconds(900);
    LocationRiskAssessmentRequested event = event(true, expiresAt);
    when(triggerService.assessLocationRisk(42L, "198.51.100.10", 99L))
        .thenReturn(new LocationRiskTriggerResult(location(), false, true));
    when(assessmentService.assess(42L, "198.51.100.10", 99L, location()))
        .thenReturn(new LocationRiskAssessment(RiskLevel.HIGH, "Unusual origin",
            AssessmentConfidence.MEDIUM, RecommendedAction.FLAG));

    listener.handle(event);

    verify(tokenDenylistService, never()).deny(any(), any());
    verify(userService, never()).invalidateRefreshTokens(any());
    verify(auditService).recordAuditLog(any(), any(), any(), any(), any());
  }

  private void verifyNoAssessmentSideEffects() {
    verify(auditService, never()).recordAuditLog(any(), any(), any(), any(), any());
    verify(tokenDenylistService, never()).deny(any(), any());
  }

  private LocationRiskAssessmentRequested event(boolean successful, Instant expiresAt) {
    return new LocationRiskAssessmentRequested(
        99L, 42L, "198.51.100.10", "test-jti", successful, expiresAt);
  }

  private IpGeolocationResult location() {
    return new IpGeolocationResult(
        "198.51.100.10", "Karachi", "Pakistan", "PK", "test", true);
  }
}
