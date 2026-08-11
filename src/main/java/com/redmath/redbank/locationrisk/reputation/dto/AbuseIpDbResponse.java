package com.redmath.redbank.locationrisk.reputation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record AbuseIpDbResponse(
    Data data
) {

  public record Data(
      @JsonProperty("ipAddress")
      String ipAddress,

      Integer abuseConfidenceScore,

      @JsonProperty("totalReports")
      Integer totalReports,

      @JsonProperty("lastReportedAt")
      Instant lastReportedAt,

      String isp,

      List<Report> reports
  ) {

  }

  public record Report(
      List<Integer> categories
  ) {

  }
}