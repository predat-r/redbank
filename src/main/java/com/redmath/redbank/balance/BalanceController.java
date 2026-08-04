package com.redmath.redbank.balance;

import com.redmath.redbank.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @GetMapping("/me/balance")
  public ResponseEntity<BalanceDto> getMyBalance(
      @AuthenticationPrincipal Jwt jwt
  ) {
    Long userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    return ResponseEntity.ok(
        BalanceDto.from(balanceService.getLatestBalanceByUserId(userId)));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{accountId}/balance")
  public ResponseEntity<BalanceDto> getBalanceByAccountHolderId(@PathVariable Long accountId) {
    return ResponseEntity.ok(
        BalanceDto.from(balanceService.getLatestBalanceByAccountHolderId(accountId)));
  }
}