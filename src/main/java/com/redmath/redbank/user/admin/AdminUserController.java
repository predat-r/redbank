package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.dto.AdminUserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

  private final AdminUserService adminUserService;

  @GetMapping
  public Page<AdminUserResponse> findUsers(@PageableDefault(size = 20, sort = {"createdAt",
      "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
    return adminUserService.findUsers(pageable);
  }

  @GetMapping("/{userId}")
  public AdminUserResponse findUser(@PathVariable Long userId) {
    return adminUserService.findUser(userId);
  }

  @PostMapping("/{userId}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activateUser(@PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
    Number adminUserIdClaim = jwt.getClaim("userId");

    adminUserService.activateUser(userId, adminUserIdClaim.longValue());
  }

  @PostMapping("/{userId}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivateUser(@PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
    Number adminUserIdClaim = jwt.getClaim("userId");
    adminUserService.deactivateUser(userId, adminUserIdClaim.longValue());
  }
}