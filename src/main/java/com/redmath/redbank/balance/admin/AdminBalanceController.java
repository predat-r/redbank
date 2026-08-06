package com.redmath.redbank.balance.admin;

import com.redmath.redbank.balance.Balance;
import com.redmath.redbank.balance.dto.BalanceDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/balance")
public class AdminBalanceController {

  private final AdminBalanceService adminBalanceService;

  public AdminBalanceController(AdminBalanceService adminBalanceService) {
    this.adminBalanceService = adminBalanceService;
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{accountId}/latest")
  public ResponseEntity<BalanceDto> getLatestBalance(@PathVariable Long accountId) {
    Balance balance = adminBalanceService.getLatestBalanceByAccountHolderId(accountId);
    return ResponseEntity.ok(BalanceDto.from(balance));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping("/{accountId}/ledger")
  public ResponseEntity<Page<BalanceDto>> getBalanceLedger(
      @PathVariable Long accountId,
      @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    Page<Balance> balancePage = adminBalanceService.getBalanceLedgerByAccountHolderId(
        accountId, pageable);

    Page<BalanceDto> balanceDtoPage = balancePage.map(BalanceDto::from);
    return ResponseEntity.ok(balanceDtoPage);
  }
}
