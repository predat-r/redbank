package com.redmath.redbank.chatbot.query;

import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.enums.Direction;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TransactionQueryService {

  private final BankTransactionRepository transactionRepository;

  public TransactionQueryService(BankTransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public AggregateResult execute(FinancialQueryIntent intent, Long myAccountHolderId) {
    Specification<BankTransaction> spec = buildSpec(intent, myAccountHolderId);
    List<BankTransaction> matches = transactionRepository.findAll(spec);

    BigDecimal sum = matches.stream()
        .map(BankTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal average = matches.isEmpty()
        ? BigDecimal.ZERO
        : sum.divide(BigDecimal.valueOf(matches.size()), 2, RoundingMode.HALF_UP);

    return new AggregateResult(sum, matches.size(), average, matches);
  }

  private Specification<BankTransaction> buildSpec(FinancialQueryIntent intent,
      Long myAccountHolderId) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(cb.equal(root.get("status"), TransactionStatus.COMPLETED));

      Predicate iAmSource = cb.equal(root.get("sourceAccountHolder").get("id"), myAccountHolderId);
      Predicate iAmDestination = cb.equal(root.get("destinationAccountHolder").get("id"),
          myAccountHolderId);

      Direction direction = intent.getDirection() == null ? Direction.BOTH : intent.getDirection();
      Long counterpartyId = intent.getCounterpartyAccountHolderId();

      if (intent.getTransactionType() != null) {
        predicates.add(cb.equal(root.get("type"), intent.getTransactionType()));
      }

      if (direction == Direction.DEBIT) {
        predicates.add(iAmSource);
        if (counterpartyId != null) {
          predicates.add(cb.equal(root.get("destinationAccountHolder").get("id"), counterpartyId));
        }
      } else if (direction == Direction.CREDIT) {
        predicates.add(iAmDestination);
        if (counterpartyId != null) {
          predicates.add(cb.equal(root.get("sourceAccountHolder").get("id"), counterpartyId));
        }
      } else { // BOTH
        predicates.add(cb.or(iAmSource, iAmDestination));

        if (counterpartyId != null) {
          Predicate theyAreDestination = cb.equal(root.get("destinationAccountHolder").get("id"),
              counterpartyId);
          Predicate theyAreSource = cb.equal(root.get("sourceAccountHolder").get("id"),
              counterpartyId);
          predicates.add(cb.or(
              cb.and(iAmSource, theyAreDestination),
              cb.and(iAmDestination, theyAreSource)
          ));
        }
      }

      if (intent.getCategory() != null) {
        predicates.add(cb.equal(root.get("category"), intent.getCategory()));
      }
      if (intent.getStartDate() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
            intent.getStartDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC)));
      }
      if (intent.getEndDate() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
            intent.getEndDate().atTime(23, 59, 59).atOffset(java.time.ZoneOffset.UTC)));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public Optional<BankTransaction> findLatestOrEarliest(FinancialQueryIntent intent,
      Long myAccountHolderId) {
    Specification<BankTransaction> spec = buildLookupSpec(intent, myAccountHolderId);
    Sort sort = "EARLIEST".equals(intent.getSortOrder())
        ? Sort.by("createdAt").ascending()
        : Sort.by("createdAt").descending();

    return transactionRepository.findAll(spec, sort).stream().findFirst();
  }

  private Specification<BankTransaction> buildLookupSpec(FinancialQueryIntent intent,
      Long myAccountHolderId) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("status"), TransactionStatus.COMPLETED));

      if (intent.getTransactionType() != null) {
        predicates.add(cb.equal(root.get("type"), intent.getTransactionType()));
      }

      Predicate iAmSource = cb.equal(root.get("sourceAccountHolder").get("id"), myAccountHolderId);
      Predicate iAmDestination = cb.equal(root.get("destinationAccountHolder").get("id"),
          myAccountHolderId);
      predicates.add(cb.or(iAmSource, iAmDestination));

      if (intent.getCategory() != null) {
        predicates.add(cb.equal(root.get("category"), intent.getCategory()));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public record AggregateResult(BigDecimal sum, long count, BigDecimal average,
                                List<BankTransaction> matches) {

    public AggregateResult {
      matches = matches != null ? List.copyOf(matches) : List.of();
    }

    @Override
    public List<BankTransaction> matches() {
      return matches != null ? List.copyOf(matches) : List.of();
    }
  }
}