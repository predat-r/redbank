package com.redmath.redbank.locationrisk.loginhistory;

import com.redmath.redbank.locationrisk.login.LoginEvent;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;

public class LoginHistoryTools {

  private final Long userId;
  private final LoginHistoryService loginHistoryService;
  private final String currentIp;
  private final Long currentLoginEventId;

  public LoginHistoryTools(
      Long userId,
      LoginHistoryService loginHistoryService,
      String currentIp,
      Long currentLoginEventId
  ) {
    this.userId = userId;
    this.loginHistoryService = loginHistoryService;
    this.currentIp = currentIp;
    this.currentLoginEventId = currentLoginEventId;
  }

  @Tool(description = "Retrieve the user's 20 most recent login attempts")
  public List<LoginEvent> getRecentLoginHistory() {
    return loginHistoryService.getLatestLoginAttemptsExcluding(
        userId,
        currentLoginEventId
    );
  }

  @Tool(description = "Retrieve the user's latest successful login")
  public LoginEvent getLatestSuccessfulLogin() {
    return loginHistoryService
        .getLatestSuccessfulLoginExcluding(userId, currentLoginEventId)
        .orElse(null);
  }

  @Tool(description = "Retrieve the user's login attempts from the current IP address")
  public List<LoginEvent> getAttemptsFromCurrentIp() {
    return loginHistoryService
        .getAttemptsByIpAddressExcluding(
            userId,
            currentIp,
            currentLoginEventId
        );
  }

  @Tool(description = "Check whether the user has previously used the current IP address")
  public boolean hasUsedCurrentIpBefore() {
    return loginHistoryService.hasUsedIpBeforeExcluding(
        userId,
        currentIp,
        currentLoginEventId
    );
  }
}
