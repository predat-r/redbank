package com.redmath.redbank.account_holder.admin;

import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderDto;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountHolderController {

  private final AdminAccountHolderService adminAccountHolderService;

  public AdminAccountHolderController(AdminAccountHolderService adminAccountHolderService) {
    this.adminAccountHolderService = adminAccountHolderService;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{accountId}")
  ResponseEntity<AccountHolderDto> getAccountHolder(
      @PathVariable Long accountId
  ) {
    AccountHolder accountHolder = adminAccountHolderService.getAccountHolderById(accountId);
    return ResponseEntity.ok(AccountHolderDto.from(accountHolder));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping
  ResponseEntity<Page<AccountHolderDto>> getAllAccountHolders(
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    Page<AccountHolder> accountHoldersPage = adminAccountHolderService.getAllAccountHolders(pageable);

    Page<AccountHolderDto> accountHolderDtoPage = accountHoldersPage.map(AccountHolderDto::from);
    return ResponseEntity.ok(accountHolderDtoPage);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/freeze/{accountId}")
  ResponseEntity<Void> freezeAccountHolder(
      @PathVariable Long accountId
  ) {
    adminAccountHolderService.freezeAccountHolder(accountId);
    return ResponseEntity.noContent().build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/deactivate/{accountId}")
  ResponseEntity<Void> deactivateAccountHolder(
      @PathVariable Long accountId
  ) {
    adminAccountHolderService.deactivateAccountHolder(accountId);
    return ResponseEntity.noContent().build();
  }
}
