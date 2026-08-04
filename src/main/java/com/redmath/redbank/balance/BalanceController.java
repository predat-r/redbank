package com.redmath.redbank.balance;

import com.redmath.redbank.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

  @GetMapping("/me/balance")
  public ResponseEntity<BalanceDto> getMyBalance(
      @AuthenticationPrincipal User user
  ) {
    if (user == null || user.getId() == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    return ResponseEntity.ok(
        BalanceDto.from(balanceService.getLatestBalanceByUserId(user.getId())));
  }

  // for admin access only
  @GetMapping("/{accountId}/balance")
  public ResponseEntity<BalanceDto> getBalanceByAccountHolderId(@PathVariable Long accountId) {
    return ResponseEntity.ok(
        BalanceDto.from(balanceService.getLatestBalanceByAccountHolderId(accountId)));
  }
}