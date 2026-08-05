package com.redmath.redbank.account;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountHolderController {

  private final AccountHolderService accountHolderService;

  public AccountHolderController(AccountHolderService accountHolderService) {
    this.accountHolderService = accountHolderService;
  }

  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @GetMapping("/me")
  ResponseEntity<AccountHolderDto> getMyAccountHolder(
      @AuthenticationPrincipal Jwt jwt
  ) {
    Long userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    AccountHolder accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    return ResponseEntity.ok(AccountHolderDto.from(accountHolder));
  }

  @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_HOLDER')")
  @GetMapping("/name/{accountNumber}")
  ResponseEntity<Map<String, Object>> getAccountHolderByAccountNumber(
      @PathVariable String accountNumber
  ) {
    String name = accountHolderService.getAccountHolderNameByAccountNumber(
        accountNumber);
    Map<String, Object> response = new HashMap<>();
    response.put("name", name);
    response.put("accountNumber", accountNumber);
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @PatchMapping("/freeze/me")
  ResponseEntity<Void> freezeMyAccountHolder(
      @AuthenticationPrincipal Jwt jwt
  ) {
    Long userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    accountHolderService.freezeMyAccountHolder(userId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  @PatchMapping("/deactivate/me")
  ResponseEntity<Void> deactivateMyAccountHolder(
      @AuthenticationPrincipal Jwt jwt
  ) {
    Long userId = jwt.getClaim("userId");
    if (userId == null) {
      throw new IllegalArgumentException("Authenticated user is required");
    }
    accountHolderService.deactivateMyAccountHolder(userId);
    return ResponseEntity.noContent().build();
  }

}
