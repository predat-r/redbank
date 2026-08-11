package com.redmath.redbank.locationrisk.ai;

import com.redmath.redbank.locationrisk.geolocation.IpGeolocationResult;
import com.redmath.redbank.locationrisk.loginhistory.LoginHistoryService;
import com.redmath.redbank.locationrisk.loginhistory.LoginHistoryTools;
import com.redmath.redbank.locationrisk.reputation.IpReputationProvider;
import com.redmath.redbank.locationrisk.reputation.IpReputationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LocationRiskAssessmentService {

  private final LoginHistoryService loginHistoryService;
  private final ChatClient chatClient;
  private final IpReputationProvider ipReputationProvider;

  public LocationRiskAssessmentService(LoginHistoryService loginHistoryService,
      @Qualifier("locationRiskChatClient") ChatClient chatClient,
      IpReputationProvider ipReputationProvider) {
    this.loginHistoryService = loginHistoryService;
    this.chatClient = chatClient;
    this.ipReputationProvider = ipReputationProvider;
  }

  public LocationRiskAssessment assess(Long userId, String currentIp,
      Long currentLoginEventId, IpGeolocationResult geolocationResult) {
    LoginHistoryTools loginHistoryTools = new LoginHistoryTools(userId, loginHistoryService,
        currentIp, currentLoginEventId);
    IpReputationTools ipReputationTools = new IpReputationTools(currentIp, ipReputationProvider);
    String systemPrompt = """
        You are a login-origin security risk assessor for a banking application.
        
        Evaluate whether the current login origin is consistent with the user's
        previous login history.
        
        Use the available login-history tools when historical context is needed.
        The tools are restricted to the current user and current IP.
        
        For every assessment, call the current-IP reputation tool and use its result as evidence.
        Also use the login-history tools to inspect the user's previous access pattern.
        If either tool returns unavailable data, state that clearly and reduce confidence.
        
        Consider only login-origin evidence:
        - Current IP geolocation.
        - Previous successful login locations.
        - Whether the current IP was previously used.
        - Recent login timestamps and locations.
        - Repeated attempts from the current IP.
        - Unusual movement between distant locations.
        
        Do not analyze transactions, balances, or spending.
        
        Do not invent missing facts. If data is unavailable, reduce confidence.
        
        Return a structured LocationRiskAssessment with:
        - riskLevel: LOW, MEDIUM, HIGH, or EXTREME
        - reason: one short evidence-based explanation
        - confidence: LOW, MEDIUM, or HIGH
        - recommendedAction: ALLOW, FLAG, CHALLENGE, or REVOKE_SESSION
        """;

    String userPrompt = """
        Assess this login origin.
        
        Current IP: %s
        Current city: %s
        Current country: %s
        Geolocation successful: %s
        """.formatted(currentIp, geolocationResult.city(), geolocationResult.country(),
        geolocationResult.successful());

    return chatClient.prompt().system(systemPrompt).user(userPrompt)
        .tools(loginHistoryTools, ipReputationTools).call()
        .entity(LocationRiskAssessment.class);
  }
}
