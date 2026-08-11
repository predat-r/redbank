package com.redmath.redbank.chatbot.query;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CounterpartyResolver {

  private final AccountHolderRepository accountHolderRepository;

  public CounterpartyResolver(AccountHolderRepository accountHolderRepository) {
    this.accountHolderRepository = accountHolderRepository;
  }

  public ResolutionResult resolve(String rawName, Long myAccountHolderId) {
    List<AccountHolder> matches = accountHolderRepository.findTransactedCounterparties(
        myAccountHolderId, rawName);

    if (matches.isEmpty()) {
      return ResolutionResult.notFound(rawName);
    }
    if (matches.size() > 1) {
      return ResolutionResult.ambiguous(matches);
    }
    return ResolutionResult.resolved(matches.get(0).getId());
  }

  public static class ResolutionResult {

    public enum Status {NOT_FOUND, AMBIGUOUS, RESOLVED}

    public Status status;
    public Long accountHolderId;
    public List<AccountHolder> candidates;

    static ResolutionResult notFound(String name) {
      var r = new ResolutionResult();
      r.status = Status.NOT_FOUND;
      return r;
    }

    static ResolutionResult ambiguous(List<AccountHolder> candidates) {
      var r = new ResolutionResult();
      r.status = Status.AMBIGUOUS;
      r.candidates = candidates;
      return r;
    }

    static ResolutionResult resolved(Long accountHolderId) {
      var r = new ResolutionResult();
      r.status = Status.RESOLVED;
      r.accountHolderId = accountHolderId;
      return r;
    }

    public List<AccountHolder> getCandidates() {
      return candidates != null ? List.copyOf(candidates) : List.of();
    }
  }
}