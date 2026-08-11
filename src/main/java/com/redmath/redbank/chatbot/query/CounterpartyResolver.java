package com.redmath.redbank.chatbot.query;

import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.account.AccountHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CounterpartyResolver {

  private final AccountHolderRepository accountHolderRepository;

  public CounterpartyResolver(AccountHolderRepository accountHolderRepository) {
    this.accountHolderRepository = accountHolderRepository;
  }

  public ResolutionResult resolve(String rawName) {
    // Adjust query to however your User/AccountHolder name field is structured
    List<AccountHolder> matches = accountHolderRepository.findByUserNameContainingIgnoreCase(rawName);

    if (matches.isEmpty()) {
      return ResolutionResult.notFound(rawName);
    }
    if (matches.size() > 1) {
      return ResolutionResult.ambiguous(matches);
    }
    return ResolutionResult.resolved(matches.get(0).getAccountNumber());
  }

  public static class ResolutionResult {
    public enum Status { NOT_FOUND, AMBIGUOUS, RESOLVED }
    public Status status;
    public String accountNumber;
    public List<AccountHolder> candidates;

    static ResolutionResult notFound(String name) {
      var r = new ResolutionResult(); r.status = Status.NOT_FOUND; return r;
    }
    static ResolutionResult ambiguous(List<AccountHolder> candidates) {
      var r = new ResolutionResult(); r.status = Status.AMBIGUOUS; r.candidates = candidates; return r;
    }
    static ResolutionResult resolved(String accountNumber) {
      var r = new ResolutionResult(); r.status = Status.RESOLVED; r.accountNumber = accountNumber; return r;
    }
  }
}