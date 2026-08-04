package com.redmath.redbank.balance;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/balance")
public class BalanceController {

  private final BalanceService balanceService;

  public BalanceController(BalanceService balanceService) {
    this.balanceService = balanceService;
  }

  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @GetMapping("/me/latest")
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
}