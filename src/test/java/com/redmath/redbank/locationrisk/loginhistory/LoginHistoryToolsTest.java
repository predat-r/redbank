package com.redmath.redbank.locationrisk.loginhistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.locationrisk.login.LoginEvent;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginHistoryToolsTest {

  @Mock
  private LoginHistoryService loginHistoryService;

  private LoginHistoryTools tools;

  @BeforeEach
  void setUp() {
    tools = new LoginHistoryTools(42L, loginHistoryService, "198.51.100.10", 99L);
  }

  @Test
  void bindsUserIpAndCurrentEventWhenReadingHistory() {
    List<LoginEvent> events = List.of(new LoginEvent());
    when(loginHistoryService.getLatestLoginAttemptsExcluding(42L, 99L)).thenReturn(events);

    assertEquals(events, tools.getRecentLoginHistory());
    verify(loginHistoryService).getLatestLoginAttemptsExcluding(42L, 99L);
  }

  @Test
  void returnsNullWhenNoPreviousSuccessfulLoginExists() {
    when(loginHistoryService.getLatestSuccessfulLoginExcluding(42L, 99L))
        .thenReturn(Optional.empty());

    assertNull(tools.getLatestSuccessfulLogin());
  }

  @Test
  void usesBoundCurrentIpForIpHistory() {
    when(loginHistoryService.hasUsedIpBeforeExcluding(42L, "198.51.100.10", 99L))
        .thenReturn(true);

    assertEquals(true, tools.hasUsedCurrentIpBefore());
    verify(loginHistoryService)
        .hasUsedIpBeforeExcluding(42L, "198.51.100.10", 99L);
  }
}
