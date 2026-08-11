package com.redmath.redbank.locationrisk.assessment;

import com.redmath.redbank.locationrisk.ai.LocationRiskAssessment;
import com.redmath.redbank.locationrisk.ai.LocationRiskAssessmentService;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerResult;
import com.redmath.redbank.locationrisk.trigger.LocationRiskTriggerService;
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
  private final LocationRiskAssessmentService locationRiskAssessmentService;

  @Async("locationRiskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(LocationRiskAssessmentRequested event) {
    LocationRiskTriggerResult triggerResult = locationRiskTriggerService.assessLocationRisk(
        event.userId(),
        event.ipAddress(),
        event.loginEventId()
    );

    if (!triggerResult.requiresAiAssessment()) {
      return;
    }

    LocationRiskAssessment assessment = locationRiskAssessmentService.assess(
        event.userId(),
        event.ipAddress(),
        event.loginEventId(),
        triggerResult.currentLocation()
    );

    log.info(
        "Location risk assessment completed for login event {}: riskLevel={}, confidence={}, action={}, reason={}",
        event.loginEventId(),
        assessment.riskLevel(),
        assessment.confidence(),
        assessment.recommendedAction(),
        assessment.reason()
    );
  }
}
