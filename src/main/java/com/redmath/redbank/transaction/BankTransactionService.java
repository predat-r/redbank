package com.redmath.redbank.transaction;


import com.redmath.redbank.account_holder.AccountHolder;
import com.redmath.redbank.account_holder.AccountHolderRepository;
import com.redmath.redbank.user.User;
import com.redmath.redbank.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BankTransactionService {

    private static final int MAX_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final BankTransactionRepository bankTransactionRepository;
    private final UserRepository userRepository;
    private final AccountHolderRepository accountHolderRepository;

    public BankTransactionService(BankTransactionRepository bankTransactionRepository,
                                  UserRepository userRepository,
                                  AccountHolderRepository accountHolderRepository) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.userRepository = userRepository;
        this.accountHolderRepository = accountHolderRepository;
    }

    public Page<BankTransaction> getTransactionsForUser(String email, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0 || size > MAX_PAGE_SIZE) ? DEFAULT_PAGE_SIZE : size;
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        AccountHolder accountHolder = accountHolderRepository.findByUser(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account holder not found"));
        return bankTransactionRepository.findBySourceAccountHolderIdOrDestinationAccountHolderId(
                accountHolder.getId(), accountHolder.getId(), PageRequest.of(safePage, safeSize));
    }
}
