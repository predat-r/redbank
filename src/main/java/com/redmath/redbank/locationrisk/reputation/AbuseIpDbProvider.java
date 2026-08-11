package com.redmath.redbank.locationrisk.reputation;

import com.redmath.redbank.locationrisk.reputation.dto.AbuseIpDbResponse;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AbuseIpDbProvider implements IpReputationProvider {

  private final RestClient restClient;
  private final String apiKey;

  public AbuseIpDbProvider(
      @Value("${reputation-lookup.base-url}") String baseUrl,
      @Value("${reputation-lookup.api-key}") String apiKey
  ) {
    this.restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build();
    this.apiKey = apiKey;
  }

  @Override
  public ReputationLookupResult lookup(String ipAddress) {
    if (ipAddress == null
        || ipAddress.isBlank()
        || isPrivateIp(ipAddress)
        || apiKey.isBlank()) {
      return unsuccessfulResult(ipAddress);
    }

    try {
      AbuseIpDbResponse response = restClient.get()
          .uri(uriBuilder -> uriBuilder
              .path("/check")
              .queryParam("ipAddress", ipAddress)
              .queryParam("maxAgeInDays", 90)
              .build())
          .header("Key", apiKey)
          .header(HttpHeaders.ACCEPT, "application/json")
          .retrieve()
          .body(AbuseIpDbResponse.class);

      if (response == null || response.data() == null) {
        return unsuccessfulResult(ipAddress);
      }

      AbuseIpDbResponse.Data data = response.data();

      return new ReputationLookupResult(
          data.ipAddress(),
          toLong(data.abuseConfidenceScore()),
          toLong(data.totalReports()),
          data.lastReportedAt(),
          extractCategories(data.reports()),
          data.isp(),
          "AbuseIPDB",
          true
      );
    } catch (RestClientException exception) {
      return unsuccessfulResult(ipAddress);
    }
  }

  private List<String> extractCategories(
      List<AbuseIpDbResponse.Report> reports
  ) {
    if (reports == null) {
      return List.of();
    }

    return reports.stream()
        .filter(Objects::nonNull)
        .flatMap(report -> report.categories() == null
            ? Stream.empty()
            : report.categories().stream())
        .map(String::valueOf)
        .distinct()
        .toList();
  }

  private Long toLong(Integer value) {
    return value == null ? null : value.longValue();
  }

  private ReputationLookupResult unsuccessfulResult(String ipAddress) {
    return new ReputationLookupResult(
        ipAddress,
        null,
        null,
        null,
        Collections.emptyList(),
        null,
        "AbuseIPDB",
        false
    );
  }

  private boolean isPrivateIp(String ipAddress) {
    try {
      InetAddress address = InetAddress.getByName(ipAddress);

      return address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isLinkLocalAddress()
          || address.isSiteLocalAddress()
          || address.isMulticastAddress();
    } catch (UnknownHostException exception) {
      return true;
    }
  }
}