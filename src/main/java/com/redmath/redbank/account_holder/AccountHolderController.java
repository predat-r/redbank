package com.redmath.redbank.account_holder;

import com.redmath.redbank.user.User;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
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

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{accountId}")
  ResponseEntity<AccountHolderDto> getAccountHolder(
      @PathVariable Long accountId
  ) {
    AccountHolder accountHolder = accountHolderService.getAccountHolderById(accountId);
    return ResponseEntity.ok(AccountHolderDto.from(accountHolder));
  }

  @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_HOLDER')")
  @GetMapping("/account-number/{accountNumber}")
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

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  ResponseEntity<Map<String, Object>> getAllAccountHolders(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir
  ) {
    Page<AccountHolder> accountHoldersPage = accountHolderService.getAllAccountHolders(page, size,
        sortBy, sortDir);
    Map<String, Object> response = new HashMap<>();
    response.put("accountHolders", accountHoldersPage.getContent().stream()
        .map(AccountHolderDto::from)
        .toList());
    response.put("currentPage", accountHoldersPage.getNumber());
    response.put("size", accountHoldersPage.getSize());
    response.put("totalItems", accountHoldersPage.getTotalElements());
    response.put("totalPages", accountHoldersPage.getTotalPages());
    response.put("isLast", accountHoldersPage.isLast());
    response.put("total", accountHoldersPage.getTotalElements());
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/freeze/{accountId}")
  ResponseEntity<Void> freezeAccountHolder(
      @PathVariable Long accountId
  ) {
    accountHolderService.freezeAccountHolder(accountId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/deactivate/{accountId}")
  ResponseEntity<Void> deactivateAccountHolder(
      @PathVariable Long accountId
  ) {
    accountHolderService.deactivateAccountHolder(accountId);
    return ResponseEntity.noContent().build();
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
