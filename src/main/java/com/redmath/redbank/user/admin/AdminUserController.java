package com.redmath.redbank.user.admin;

import com.redmath.redbank.user.admin.dto.AdminUserResponse;
import com.redmath.redbank.user.admin.dto.CreateUserRequest;
import com.redmath.redbank.user.admin.dto.CreateUserResponse;
import com.redmath.redbank.user.admin.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

  private final AdminUserService adminUserService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateUserResponse createUser(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody CreateUserRequest request
  ) {
    return adminUserService.createUser(extractUserId(jwt), request);
  }

  @GetMapping
  public Page<AdminUserResponse> findUsers(
      @PageableDefault(
          size = 20,
          sort = {"createdAt", "id"},
          direction = Sort.Direction.ASC
      ) Pageable pageable
  ) {
    return adminUserService.findUsers(pageable);
  }

  @GetMapping("/{userId}")
  public AdminUserResponse findUser(@PathVariable Long userId) {
    return adminUserService.findUser(userId);
  }

  @PutMapping("/{userId}")
  public AdminUserResponse updateUser(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long userId,
      @Valid @RequestBody UpdateUserRequest request
  ) {
    return adminUserService.updateUser(extractUserId(jwt), userId, request);
  }

  @PatchMapping("/{userId}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivateUser(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long userId
  ) {
    adminUserService.deactivateUser(extractUserId(jwt), userId);
  }

  @PatchMapping("/{userId}/reactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reactivateUser(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long userId
  ) {
    adminUserService.reactivateUser(extractUserId(jwt), userId);
  }

  private long extractUserId(Jwt jwt) {
    Object claim = jwt.getClaim("userId");

    if (!(claim instanceof Number userId)) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }

    return userId.longValue();
  }
}
