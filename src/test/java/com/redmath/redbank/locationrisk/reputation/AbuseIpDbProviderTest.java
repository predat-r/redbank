package com.redmath.redbank.locationrisk.reputation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class AbuseIpDbProviderTest {

  @Test
  void rejectsBlankIp() {
    AbuseIpDbProvider provider = new AbuseIpDbProvider("https://example.test", "api-key");

    ReputationLookupResult result = provider.lookup(" ");

    assertFalse(result.lookupSuccessful());
  }

  @Test
  void rejectsPrivateIp() {
    AbuseIpDbProvider provider = new AbuseIpDbProvider("https://example.test", "api-key");

    ReputationLookupResult result = provider.lookup("127.0.0.1");

    assertFalse(result.lookupSuccessful());
  }

  @Test
  void rejectsPublicIpWhenApiKeyIsMissing() {
    AbuseIpDbProvider provider = new AbuseIpDbProvider("https://example.test", "");

    ReputationLookupResult result = provider.lookup("198.51.100.10");

    assertFalse(result.lookupSuccessful());
  }
}
