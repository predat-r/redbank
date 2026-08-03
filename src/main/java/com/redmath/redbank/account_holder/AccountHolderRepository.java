package com.redmath.redbank.account_holder;

import com.redmath.redbank.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {
    Optional<AccountHolder> findByUser(User user);

    Optional<AccountHolder> findByAccountNumber(String accountNumber);
}
