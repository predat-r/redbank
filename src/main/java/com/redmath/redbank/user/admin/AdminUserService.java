package com.redmath.redbank.user.admin;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.account.admin.AdminAccountHolderService;
import com.redmath.redbank.account.dto.AccountHolderDto;
import com.redmath.redbank.audit.AuditAction;
import com.redmath.redbank.audit.AuditService;
import com.redmath.redbank.audit.AuditTargetType;
import com.redmath.redbank.common.exception.DuplicateUserException;
import com.redmath.redbank.common.exception.InvalidSortException;
import com.redmath.redbank.common.exception.UserAccountNotActiveException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.UserStatus;
import com.redmath.redbank.user.admin.dto.AdminUserResponse;
import com.redmath.redbank.user.admin.dto.CreateUserRequest;
import com.redmath.redbank.user.admin.dto.CreateUserResponse;
import com.redmath.redbank.user.admin.dto.UpdateUserRequest;
import com.redmath.redbank.user.role.Role;
import com.redmath.redbank.user.role.RoleName;
import com.redmath.redbank.user.role.RoleRepository;
import com.redmath.redbank.user.role.UserRole;
import com.redmath.redbank.user.role.UserRoleId;
import com.redmath.redbank.user.role.UserRoleRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "id");

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final UserRoleRepository userRoleRepository;
  private final AccountHolderService accountHolderService;
  private final AdminAccountHolderService adminAccountHolderService;
  private final AuditService auditService;

  @Transactional
  public CreateUserResponse createUser(Long adminUserId, CreateUserRequest request) {
    User admin = requireActiveAdmin(adminUserId);
    String email = normalizeEmail(request.email());
    String phoneNumber = request.phoneNumber().trim();

    requireUniqueEmail(email);
    requireUniquePhoneNumber(phoneNumber);

    Instant now = Instant.now();
    User user = User.builder()
        .email(email)
        .phoneNumber(phoneNumber)
        .passwordHash(passwordEncoder.encode(request.password()))
        .name(request.name().trim())
        .address(request.address().trim())
        .status(UserStatus.ACTIVE)
        .approvedBy(admin)
        .approvedAt(now)
        .createdAt(now)
        .updatedAt(now)
        .build();

    User savedUser = userService.save(user);
    assignAccountHolderRole(savedUser, now);
    AccountHolder accountHolder = accountHolderService.createAccountHolder(savedUser);

    auditService.recordAuditLog(adminUserId, AuditAction.USER_CREATED, AuditTargetType.USER,
        savedUser.getId().toString(), null);
    auditService.recordAuditLog(adminUserId, AuditAction.ACCOUNT_CREATED, AuditTargetType.ACCOUNT,
        accountHolder.getId().toString(), null);

    return new CreateUserResponse(toResponse(savedUser), AccountHolderDto.from(accountHolder));
  }

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
  public AdminUserResponse updateUser(Long adminUserId, Long userId,
      UpdateUserRequest request) {
    requireActiveAdmin(adminUserId);
    User user = userService.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);
    String email = normalizeEmail(request.email());
    String phoneNumber = request.phoneNumber().trim();

    if (!user.getEmail().equalsIgnoreCase(email)) {
      requireUniqueEmail(email);
    }
    if (!user.getPhoneNumber().equals(phoneNumber)) {
      requireUniquePhoneNumber(phoneNumber);
    }

    user.updateDetails(email, phoneNumber, request.name().trim(), request.address().trim(),
        Instant.now());
    auditService.recordAuditLog(adminUserId, AuditAction.USER_UPDATED, AuditTargetType.USER,
        userId.toString(), null);

    return toResponse(user);
  }

  @Transactional
  public void deactivateUser(Long adminUserId, Long userId) {
    requireActiveAdmin(adminUserId);
    User user = userService.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);

    if (!userService.deactivateUser(userId)) {
      return;
    }

    accountHolderService.findByUser(user)
        .ifPresent(accountHolder -> adminAccountHolderService.deactivateAccountHolder(
            accountHolder.getId(), adminUserId));
    auditService.recordAuditLog(adminUserId, AuditAction.USER_DEACTIVATED,
        AuditTargetType.USER, userId.toString(), null);
  }

  @Transactional
  public void reactivateUser(Long adminUserId, Long userId) {
    requireActiveAdmin(adminUserId);
    User user = userService.findByIdForUpdate(userId).orElseThrow(UserNotFoundException::new);

    if (!userService.reactivateUser(userId)) {
      return;
    }

    accountHolderService.findByUser(user)
        .ifPresent(accountHolder -> adminAccountHolderService.reactivateAccountHolder(
            accountHolder.getId(), adminUserId));
    auditService.recordAuditLog(adminUserId, AuditAction.USER_ACTIVATED,
        AuditTargetType.USER, userId.toString(), null);
  }

  private User requireActiveAdmin(Long adminUserId) {
    return userService.findById(adminUserId)
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .orElseThrow(UserAccountNotActiveException::new);
  }

  private void requireUniqueEmail(String email) {
    if (userService.existsByEmail(email)) {
      throw new DuplicateUserException("Email is already registered");
    }
  }

  private void requireUniquePhoneNumber(String phoneNumber) {
    if (userService.existsByPhoneNumber(phoneNumber)) {
      throw new DuplicateUserException("Phone number is already registered");
    }
  }

  private void assignAccountHolderRole(User user, Instant assignedAt) {
    Role role = roleRepository.findByName(RoleName.ACCOUNT_HOLDER)
        .orElseThrow(() -> new IllegalStateException("ACCOUNT_HOLDER role is not configured"));
    UserRoleId id = new UserRoleId(user.getId(), role.getId());
    UserRole userRole = UserRole.builder()
        .id(id)
        .user(user)
        .role(role)
        .assignedAt(assignedAt)
        .build();
    userRoleRepository.save(userRole);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
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
