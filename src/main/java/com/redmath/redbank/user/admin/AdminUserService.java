package com.redmath.redbank.user.admin;

import com.redmath.redbank.common.exception.InvalidSortException;
import com.redmath.redbank.common.exception.UserNotFoundException;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserService;
import com.redmath.redbank.user.dto.AdminUserResponse;
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
