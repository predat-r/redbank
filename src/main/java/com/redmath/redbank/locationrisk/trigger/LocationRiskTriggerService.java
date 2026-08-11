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

  public LocationRiskTriggerResult assessLocationRisk(Long userId, String ip) {
    IpGeolocationResult ipGeolocationResult = ipGeolocationProvider.resolve(ip);
    Optional<LoginEvent> loginEvent = loginHistoryService.getLatestSuccessfulLogin(userId);

    if (!ipGeolocationResult.successful()) {

      return new LocationRiskTriggerResult(ipGeolocationResult, null, false, false);

    }
    if (loginEvent.isEmpty()) {
      return new LocationRiskTriggerResult(ipGeolocationResult, null, false, false);

    } else {
      boolean hasUsedIpBefore = loginHistoryService.hasUsedIpBefore(userId, ip);
      if (hasUsedIpBefore || Objects.equals(loginEvent.get().getCity(),
          ipGeolocationResult.city())) {
        return new LocationRiskTriggerResult(ipGeolocationResult, loginEvent.get(), hasUsedIpBefore,
            false);
      } else {
        return new LocationRiskTriggerResult(ipGeolocationResult, loginEvent.get(), false,
            true);
      }
    }

  }
}
