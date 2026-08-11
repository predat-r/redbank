package com.redmath.redbank.chatbot.query;

import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.enums.Direction;
import com.redmath.redbank.transaction.BankTransaction;
import com.redmath.redbank.transaction.BankTransactionRepository;
import com.redmath.redbank.transaction.TransactionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionQueryService {

  private final BankTransactionRepository transactionRepository;

  public TransactionQueryService(BankTransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public AggregateResult execute(FinancialQueryIntent intent, String myAccountNumber) {
    Specification<BankTransaction> spec = buildSpec(intent, myAccountNumber);
    List<BankTransaction> matches = transactionRepository.findAll(spec);

    BigDecimal sum = matches.stream()
        .map(BankTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal average = matches.isEmpty()
        ? BigDecimal.ZERO
        : sum.divide(BigDecimal.valueOf(matches.size()), 2, RoundingMode.HALF_UP);

    return new AggregateResult(sum, matches.size(), average, matches);
  }

  private Specification<BankTransaction> buildSpec(FinancialQueryIntent intent, String myAccountNumber) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Only count completed transactions
      predicates.add(cb.equal(root.get("status"), TransactionStatus.COMPLETED));

      // Join to AccountHolder for source and destination
      Join<Object, Object> sourceJoin = root.join("sourceAccountHolder", JoinType.LEFT);
      Join<Object, Object> destJoin = root.join("destinationAccountHolder", JoinType.LEFT);

      // Direction, derived from whether "my account" is source or destination
      Direction direction = intent.getDirection() == null ? Direction.BOTH : intent.getDirection();
      String counterparty = intent.getCounterpartyAccountNumber();

      Predicate iAmSource = cb.equal(sourceJoin.get("accountNumber"), myAccountNumber);
      Predicate iAmDestination = cb.equal(destJoin.get("accountNumber"), myAccountNumber);

      if (intent.getTransactionType() != null) {
        predicates.add(cb.equal(root.get("type"), intent.getTransactionType()));
      }

      if (direction == Direction.DEBIT) {
        predicates.add(iAmSource);
        if (counterparty != null) {
          predicates.add(cb.equal(destJoin.get("accountNumber"), counterparty));
        }
      } else if (direction == Direction.CREDIT) {
        predicates.add(iAmDestination);
        if (counterparty != null) {
          predicates.add(cb.equal(sourceJoin.get("accountNumber"), counterparty));
        }
      } else { // BOTH
        predicates.add(cb.or(iAmSource, iAmDestination));

        if (counterparty != null) {
          Predicate theyAreDestination = cb.equal(destJoin.get("accountNumber"), counterparty);
          Predicate theyAreSource = cb.equal(sourceJoin.get("accountNumber"), counterparty);
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

  public record AggregateResult(BigDecimal sum, long count, BigDecimal average, List<BankTransaction> matches) {}
}