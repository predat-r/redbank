package com.redmath.redbank.account_holder;

import com.redmath.redbank.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository for {@link AccountHolder} entity.
 */
@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

    /**
     * Find the account holder associated with a given user.
     *
     * @param user the user
     * @return an Optional containing the found account holder, or empty
     */
    Optional<AccountHolder> findByUser(User user);
}
