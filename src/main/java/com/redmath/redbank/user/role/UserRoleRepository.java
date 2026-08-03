package com.redmath.redbank.user.role;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository
    extends JpaRepository<UserRole, UserRoleId> {
}
