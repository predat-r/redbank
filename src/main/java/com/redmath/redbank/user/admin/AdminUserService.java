package com.redmath.redbank.user.admin;

import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.common.exception.InvalidSortException;
import com.redmath.redbank.common.exception.InvalidUserStatusTransitionException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.dto.AdminUserResponse;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");
  private final AuditService auditService;
  private final UserRoleRepository userRoleRepository;
  private final UserService userService;

  @Transactional(readOnly = true)
  public Page<AdminUserResponse> findUsers(Pageable pageable) {
    validateSort(pageable);

    return userService.findAll(pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public AdminUserResponse findUser(Long userId) {
    User user = userService.findById(userId).orElseThrow(UserNotFoundException::new);

    return toResponse(user);
  }

  @Transactional
  public void activateUser(Long userId, Long adminUserId) {
    User user = userService.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);

    if (user.getStatus() == UserStatus.ACTIVE) {
      return;
    }

    if (user.getStatus() != UserStatus.DEACTIVATED) {
      throw new InvalidUserStatusTransitionException("Only deactivated users can be activated");
    }

    user.activate(Instant.now());
    auditService.record(adminUserId, AuditAction.USER_ACTIVATED, AuditTargetType.USER,
        userId.toString(), null);
  }

  @Transactional
  public void deactivateUser(Long userId, Long adminUserId) {
    User user = userService.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);

    if (user.getStatus() == UserStatus.DEACTIVATED) {
      return;
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new InvalidUserStatusTransitionException("Only active users can be deactivated");
    }

    boolean isAdmin = userRoleRepository.existsByUser_IdAndRole_Name(userId, RoleName.ADMIN);

    if (isAdmin) {
      throw new InvalidUserStatusTransitionException(
          "Administrators cannot be deactivated through user management");
    }

    user.deactivate(Instant.now());

    auditService.record(adminUserId, AuditAction.USER_DEACTIVATED, AuditTargetType.USER,
        userId.toString(), null);
  }

  private AdminUserResponse toResponse(User user) {
    return new AdminUserResponse(user.getId(), user.getEmail(), user.getPhoneNumber(),
        user.getName(), user.getAddress(), user.getStatus(), user.getCreatedAt(),
        user.getUpdatedAt());
  }

  private void validateSort(Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
      if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
        throw new InvalidSortException();
      }
    }
  }
}
