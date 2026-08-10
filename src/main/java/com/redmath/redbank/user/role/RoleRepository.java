package com.redmath.redbank.user.role;

import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  @Cacheable(
      cacheNames = "role-by-name",
      key = "#name",
      unless = "#result == null"
  )
  Optional<Role> findByName(RoleName name);
}
