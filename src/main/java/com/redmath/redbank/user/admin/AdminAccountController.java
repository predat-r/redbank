package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.dto.AdminAccountResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminAccountController {

  private final AdminAccountService adminAccountService;

  @GetMapping
  public Page<AdminAccountResponse> findAccounts(
      @PageableDefault(
          size = 20,
          sort = "createdAt",
          direction = Sort.Direction.DESC
      )
      Pageable pageable
  ) {
    return adminAccountService.findAccounts(pageable);
  }

  @GetMapping("/{accountNumber}")
  public AdminAccountResponse findAccount(
      @PathVariable String accountNumber
  ) {
    return adminAccountService.findAccount(accountNumber);
  }

  @PostMapping("/{accountNumber}/freeze")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void freezeAccount(@PathVariable String accountNumber) {
    adminAccountService.freezeAccount(accountNumber);
  }

  @PostMapping("/{accountNumber}/close")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void closeAccount(@PathVariable String accountNumber) {
    adminAccountService.closeAccount(accountNumber);
  }

}