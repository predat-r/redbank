package com.redmath.redbank.locationrisk.reputation;

import java.time.Instant;
import java.util.List;

public record ReputationLookupResult(

    String ipAddress, Long abuseConfidenceScore, Long numberOfReports,
    Instant lastReportedTimestamp, List<String> abuseCategories, String isp, String providerName,
    boolean lookupSuccessful) {

  public ReputationLookupResult {
    abuseCategories = abuseCategories == null ? List.of() : List.copyOf(abuseCategories);
  }
}
