package com.redmath.redbank.locationrisk.reputation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IpReputationToolsTest {

  @Mock
  private IpReputationProvider provider;

  @Test
  void checksOnlyTheBoundCurrentIp() {
    ReputationLookupResult expected = new ReputationLookupResult(
        "198.51.100.10", 25L, 2L, null, java.util.List.of("14"), "Example ISP",
        "AbuseIPDB", true);
    when(provider.lookup("198.51.100.10")).thenReturn(expected);

    IpReputationTools tools = new IpReputationTools("198.51.100.10", provider);

    assertEquals(expected, tools.checkCurrentIpReputation());
    verify(provider).lookup("198.51.100.10");
  }
}
