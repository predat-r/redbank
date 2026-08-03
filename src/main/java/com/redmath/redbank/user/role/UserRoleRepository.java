package com.redmath.redbank.user.role;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

  @EntityGraph(attributePaths = "role")
  List<UserRole> findAllByUser_Id(Long userId);
}
