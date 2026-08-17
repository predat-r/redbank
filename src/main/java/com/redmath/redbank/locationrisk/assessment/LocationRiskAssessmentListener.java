package com.redmath.redbank.locationrisk.assessment;

import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.locationrisk.login.LoginEventService;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerResult;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerService;
import com.redmath.redbank.security.denylist.TokenDenylistService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationRiskAssessmentListener {

  private final LocationRiskTriggerService locationRiskTriggerService;
  private final LoginEventService loginEventService;
  private final LocationRiskAssessmentService locationRiskAssessmentService;
  private final AuditService auditService;
  private final TokenDenylistService tokenDenylistService;

  @Async("locationRiskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(LocationRiskAssessmentRequested event) {
    if (!event.loginSuccessful()) {
      return;
    }

    LocationRiskTriggerResult triggerResult = locationRiskTriggerService.assessLocationRisk(
        event.userId(),
        event.ipAddress(),
        event.loginEventId()
    );

    if (triggerResult.currentLocation().successful()) {
      loginEventService.updateLocation(
          event.loginEventId(),
          triggerResult.currentLocation().city(),
          triggerResult.currentLocation().country()
      );
    }

    if (!triggerResult.requiresAiAssessment()) {
      return;
    }

    LocationRiskAssessment assessment = locationRiskAssessmentService.assess(
        event.userId(),
        event.ipAddress(),
        event.loginEventId(),
        triggerResult.currentLocation()
    );

    if (log.isInfoEnabled()) {
      log.info(
          "Location risk assessment completed for login event {}: riskLevel={}, confidence={}, action={}, reason={}",
          event.loginEventId(),
          assessment.riskLevel(),
          assessment.confidence(),
          assessment.recommendedAction(),
          assessment.reason()
      );
    }

    String details = "riskLevel=" + assessment.riskLevel()
        + ", confidence=" + assessment.confidence()
        + ", action=" + assessment.recommendedAction()
        + ", reason=" + assessment.reason();

    if ("EXTREME".equals(assessment.riskLevel())
        && "REVOKE_SESSION".equals(assessment.recommendedAction())
        && event.accessTokenJti() != null
        && !event.accessTokenJti().isBlank()
        && event.expiresAt() != null) {
      Duration remainingTokenLifetime = Duration.between(Instant.now(), event.expiresAt());
      if (!remainingTokenLifetime.isNegative() && !remainingTokenLifetime.isZero()) {
        tokenDenylistService.deny(event.accessTokenJti(), remainingTokenLifetime);
      }
    }

    auditService.recordAuditLog(
        event.userId(),
        AuditAction.LOCATION_RISK_ASSESSED,
        AuditTargetType.LOGIN_EVENT,
        event.loginEventId().toString(),
        details
    );
  }
}
