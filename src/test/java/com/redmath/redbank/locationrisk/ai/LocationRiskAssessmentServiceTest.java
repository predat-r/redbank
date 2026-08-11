package com.redmath.redbank.locationrisk.ai;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.loginhistory.LoginHistoryService;
import com.redmath.redbank.locationrisk.reputation.IpReputationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class LocationRiskAssessmentServiceTest {

  @Mock
  private LoginHistoryService loginHistoryService;

  @Mock
  private IpReputationProvider reputationProvider;

  @Mock
  private ChatClient chatClient;

  @Mock
  private ChatClient.ChatClientRequestSpec requestSpec;

  @Mock
  private ChatClient.CallResponseSpec responseSpec;

  @Test
  void sendsBoundToolsAndMapsStructuredAiResult() {
    LocationRiskAssessment expected = new LocationRiskAssessment(
        "HIGH", "New city and poor IP reputation", "HIGH", "CHALLENGE");
    when(chatClient.prompt()).thenReturn(requestSpec);
    when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
    when(requestSpec.tools(any(), any())).thenReturn(requestSpec);
    when(requestSpec.call()).thenReturn(responseSpec);
    when(responseSpec.entity(eq(LocationRiskAssessment.class), any()))
        .thenReturn(expected);

    LocationRiskAssessmentService service = new LocationRiskAssessmentService(
        loginHistoryService, chatClient, reputationProvider);

    IpGeolocationResult location = new IpGeolocationResult(
        "198.51.100.10", "Karachi", "Pakistan", "PK", "test", true);

    assertSame(expected, service.assess(42L, "198.51.100.10", 99L, location));
    verify(requestSpec).tools(any(), any());
    verify(responseSpec).entity(eq(LocationRiskAssessment.class), any());
  }
}
