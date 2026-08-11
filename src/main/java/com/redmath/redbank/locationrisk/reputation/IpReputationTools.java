package com.redmath.redbank.locationrisk.reputation;

import org.springframework.ai.tool.annotation.Tool;

public class IpReputationTools {

  private final String currentIp;
  private final IpReputationProvider ipReputationProvider;

  public IpReputationTools(
      String currentIp,
      IpReputationProvider ipReputationProvider
  ) {
    this.currentIp = currentIp;
    this.ipReputationProvider = ipReputationProvider;
  }

  @Tool(description = "Check the abuse reputation of the current login IP address")
  public ReputationLookupResult checkCurrentIpReputation() {
    return ipReputationProvider.lookup(currentIp);
  }
}