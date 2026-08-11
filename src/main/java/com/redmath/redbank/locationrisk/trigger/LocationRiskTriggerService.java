package com.redmath.redbank.locationrisk.trigger;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationProvider;
import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.login.LoginEvent;
import com.redmath.redbank.locationrisk.loginhistory.LoginHistoryService;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class LocationRiskTriggerService {

  private final IpGeolocationProvider ipGeolocationProvider;
  private final LoginHistoryService loginHistoryService;


  public LocationRiskTriggerService(IpGeolocationProvider ipGeolocationProvider,
      LoginHistoryService loginHistoryService) {
    this.ipGeolocationProvider = ipGeolocationProvider;
    this.loginHistoryService = loginHistoryService;
  }

  public LocationRiskTriggerResult assessLocationRisk(
      Long userId,
      String ip,
      Long currentLoginEventId
  ) {
    IpGeolocationResult ipGeolocationResult = ipGeolocationProvider.resolve(ip);

    if (!ipGeolocationResult.successful()) {
      return new LocationRiskTriggerResult(ipGeolocationResult, false, false);
    }

    Optional<LoginEvent> loginEvent = loginHistoryService
        .getLatestSuccessfulLoginExcluding(userId, currentLoginEventId);

    if (loginEvent.isEmpty()) {
      return new LocationRiskTriggerResult(ipGeolocationResult, false, false);
    }

    boolean hasUsedIpBefore = loginHistoryService.hasUsedIpBeforeExcluding(
        userId,
        ip,
        currentLoginEventId
    );
    if (hasUsedIpBefore || Objects.equals(loginEvent.get().getCity(), ipGeolocationResult.city())) {
      return new LocationRiskTriggerResult(
          ipGeolocationResult, hasUsedIpBefore, false);
    }

    return new LocationRiskTriggerResult(ipGeolocationResult, false, true);
  }
}
