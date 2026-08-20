package com.redmath.redbank.user;

import com.redmath.redbank.user.dto.UpdateMyProfileRequest;
import com.redmath.redbank.user.dto.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PatchMapping(value = "/me", consumes = "application/json")
  @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
  public UserDto updateMyProfile(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody UpdateMyProfileRequest request
  ) {
    return UserDto.from(userService.updateMyProfile(extractUserId(jwt), request));
  }

  private long extractUserId(Jwt jwt) {
    Object claim = jwt.getClaim("userId");
    if (!(claim instanceof Number userId)) {
      throw new IllegalArgumentException("User ID missing from authentication token");
    }
    return userId.longValue();
  }
}
