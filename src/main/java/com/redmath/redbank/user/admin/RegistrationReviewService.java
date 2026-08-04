package com.redmath.redbank.user.admin;

import com.redmath.redbank.account_holder.AccountHolderService;
import com.redmath.redbank.common.exception.InvalidSortException;
import com.redmath.redbank.common.exception.RegistrationAlreadyReviewedException;
import com.redmath.redbank.common.exception.RegistrationNotFoundException;
import com.redmath.redbank.common.exception.UserAccountNotActiveException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.dto.PendingRegistrationResponse;
import com.redmath.redbank.user.dto.RejectRegistrationRequest;
import com.redmath.redbank.user.role.Role;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.RoleRepository;
import com.redmath.redbank.user.role.UserRole;
import com.redmath.redbank.user.role.UserRoleId;
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
public class RegistrationReviewService {

  private static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of("createdAt", "id");

  private final UserService userService;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final AccountHolderService accountHolderService;

  @Transactional
  public void approveRegistration(Long userId, Long adminUserId) {
    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(RegistrationNotFoundException::new);

    if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
      throw new RegistrationAlreadyReviewedException();
    }

    User admin = userService.findById(adminUserId)
        .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
        .orElseThrow(UserAccountNotActiveException::new);

    Role accountHolderRole = roleRepository.findByName(RoleName.ACCOUNT_HOLDER)
        .orElseThrow(() ->
            new IllegalStateException("ACCOUNT_HOLDER role is not configured")
        );

    Instant now = Instant.now();

    user.approveRegistration(admin, now);

    UserRoleId userRoleId = new UserRoleId(
        user.getId(),
        accountHolderRole.getId()
    );

    if (!userRoleRepository.existsById(userRoleId)) {
      UserRole userRole = UserRole.builder()
          .id(userRoleId)
          .user(user)
          .role(accountHolderRole)
          .assignedAt(now)
          .build();

      userRoleRepository.save(userRole);
    }

    accountHolderService.createAccountHolder(userId);
  }

  @Transactional(readOnly = true)
  public Page<PendingRegistrationResponse> findPendingRegistrations(Pageable pageable) {
    validateSort(pageable);

    return userService
        .findAllByStatus(UserStatus.PENDING_APPROVAL, pageable)
        .map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public PendingRegistrationResponse findPendingRegistration(Long userId) {
    User user = userService.findById(userId)
        .orElseThrow(RegistrationNotFoundException::new);

    if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
      throw new RegistrationAlreadyReviewedException();
    }

    return toResponse(user);
  }

  @Transactional
  public void rejectRegistration(
      Long userId,
      RejectRegistrationRequest request
  ) {
    User user = userService.findByIdForUpdate(userId)
        .orElseThrow(RegistrationNotFoundException::new);

    if (user.getStatus() != UserStatus.PENDING_APPROVAL) {
      throw new RegistrationAlreadyReviewedException();
    }

    String rejectionReason = request.rejectionReason();

    if (rejectionReason != null) {
      rejectionReason = rejectionReason.trim();

      if (rejectionReason.isEmpty()) {
        rejectionReason = null;
      }
    }

    user.rejectRegistration(rejectionReason, Instant.now());
  }

  private PendingRegistrationResponse toResponse(User user) {
    return new PendingRegistrationResponse(
        user.getId(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.getName(),
        user.getAddress(),
        user.getStatus(),
        user.getCreatedAt()
    );
  }

  private void validateSort(Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
      if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
        throw new InvalidSortException();
      }
    }
  }
}