package com.redmath.redbank.chatbot;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderService;
import com.redmath.redbank.chatbot.dto.ChatResponse;
import com.redmath.redbank.chatbot.dto.FinancialQueryIntent;
import com.redmath.redbank.chatbot.enums.QueryType;
import com.redmath.redbank.chatbot.llm.LlmClient;
import com.redmath.redbank.chatbot.query.BalanceQueryService;
import com.redmath.redbank.chatbot.query.CounterpartyResolver;
import com.redmath.redbank.chatbot.query.TransactionQueryService;
import com.redmath.redbank.transaction.BankTransaction;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

  private final LlmClient llmClient;
  private final TransactionQueryService transactionQueryService;
  private final BalanceQueryService balanceQueryService;
  private final CounterpartyResolver counterpartyResolver;
  private final AccountHolderService accountHolderService;

  public ChatbotService(LlmClient llmClient, TransactionQueryService transactionQueryService,
      BalanceQueryService balanceQueryService, CounterpartyResolver counterpartyResolver,
      AccountHolderService accountHolderService) {
    this.llmClient = llmClient;
    this.transactionQueryService = transactionQueryService;
    this.balanceQueryService = balanceQueryService;
    this.counterpartyResolver = counterpartyResolver;
    this.accountHolderService = accountHolderService;
  }

  public ChatResponse handle(String message, Long userId) {
    if (userId == null) {
      return new ChatResponse(
          "I couldn't identify your account. Please sign in again and try once more.", false);
    }

    final AccountHolder accountHolder;
    try {
      accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    } catch (RuntimeException e) {
      return new ChatResponse(
          "I couldn't load your account details right now. Please try again later.", false);
    }

    String myAccountNumber = accountHolder.getAccountNumber();
    Long accountHolderId = accountHolder.getId();
    String conversationId = String.valueOf(accountHolderId);

    FinancialQueryIntent intent = llmClient.parseIntent(message, LocalDate.now(), conversationId);

    if (intent.isNeedsClarification()) {
      return new ChatResponse(intent.getClarificationQuestion(), true);
    }

    if (intent.getQueryType() == QueryType.ADVICE_REQUEST) {
      return new ChatResponse(
          "I can show you your spending trends and balance history, but I can't give personal " +
              "financial advice. Ask me something like \"how much did I spend on groceries this month?\"",
          false);
    }

    if (intent.getQueryType() == QueryType.UNSUPPORTED) {
      return new ChatResponse(
          "I can only answer questions about your own transactions and balance.", false);
    }

    // Resolve counterparty name -> account number, if the question mentioned a person
    if (intent.getCounterpartyName() != null) {
      var resolution = counterpartyResolver.resolve(intent.getCounterpartyName(), accountHolderId);
      switch (resolution.status) {
        case NOT_FOUND:
          return new ChatResponse("I couldn't find anyone named \"" + intent.getCounterpartyName()
              + "\" in your transaction history.", false);
        case AMBIGUOUS:
          return new ChatResponse("I found multiple people named \"" + intent.getCounterpartyName()
              + "\". Could you give me more detail (e.g. full name)?", true);
        case RESOLVED:
          intent.setCounterpartyAccountHolderId(resolution.accountHolderId);
      }
    }

    return switch (intent.getQueryType()) {
      case TRANSACTION_AGGREGATE ->
          handleTransactionAggregate(intent, accountHolderId, message, conversationId);
      case BALANCE_AT_DATE -> handleBalanceAtDate(intent, accountHolderId);
      case PROJECTION -> handleProjection(accountHolderId, message, conversationId);
      case TRANSACTION_LOOKUP -> handleTransactionLookup(intent, accountHolderId);
      default -> new ChatResponse("Sorry, I couldn't process that question.", false);
    };
  }

  private ChatResponse handleTransactionAggregate(FinancialQueryIntent intent,
      Long myAccountHolderId,
      String originalMessage, String conversationId) {
    var result = transactionQueryService.execute(intent, myAccountHolderId);

    if (result.count() == 0) {
      return new ChatResponse("I didn't find any matching transactions for your question.", false);
    }

    // Build a compact factual summary to pass to the phrasing LLM
    String categoryPart = intent.getCategory() != null ? ", category: " + intent.getCategory() : "";
    String periodPart = (intent.getStartDate() != null && intent.getEndDate() != null)
        ? ", period: " + intent.getStartDate() + " to " + intent.getEndDate() : "";
    String counterpartyPart = intent.getCounterpartyName() != null
        ? ", counterparty: " + intent.getCounterpartyName() : "";
    String directionPart =
        intent.getDirection() != null ? ", direction: " + intent.getDirection() : "";

    String rawFacts = "Transaction aggregate result -"
        + directionPart + categoryPart + periodPart + counterpartyPart
        + ". Total amount: $" + result.sum()
        + ", number of transactions: " + result.count()
        + ", average per transaction: $" + result.average() + ".";

    String naturalReply = llmClient.phraseAnswer(originalMessage, rawFacts, conversationId);
    return new ChatResponse(naturalReply, false);
  }

  private ChatResponse handleBalanceAtDate(FinancialQueryIntent intent, Long accountHolderId) {
    return balanceQueryService.getBalanceAsOf(accountHolderId, intent.getAsOfDate())
        .map(balance -> new ChatResponse(
            "Your balance on " + intent.getAsOfDate() + " was $" + balance + ".", false))
        .orElse(new ChatResponse(
            "I don't have a balance record on or before " + intent.getAsOfDate() + ".", false));
  }

  private ChatResponse handleProjection(Long accountHolderId, String originalMessage,
      String conversationId) {
    var projected = balanceQueryService.projectMonthEndBalance(accountHolderId);

    String rawFacts =
        "Projected month-end balance based on the last 30 days of spending patterns: $"
            + projected + ". This is an estimate, not a guarantee.";

    String naturalReply = llmClient.phraseAnswer(originalMessage, rawFacts, conversationId);
    return new ChatResponse(naturalReply, false);
  }

  private ChatResponse handleTransactionLookup(FinancialQueryIntent intent,
      Long myAccountHolderId) {
    var result = transactionQueryService.findLatestOrEarliest(intent, myAccountHolderId);

    if (result.isEmpty()) {
      String typeLabel = intent.getTransactionType() != null
          ? intent.getTransactionType().name().toLowerCase() : "transaction";
      return new ChatResponse("I couldn't find any " + typeLabel + " on your account.", false);
    }

    BankTransaction txn = result.get();
    String typeLabel = intent.getTransactionType() != null
        ? intent.getTransactionType().name().toLowerCase() : "transaction";
    String when = intent.getSortOrder().equals("EARLIEST") ? "first" : "most recent";

    String reply = String.format("Your %s %s was $%s on %s (ref: %s).",
        when, typeLabel, txn.getAmount(), txn.getCreatedAt().toLocalDate(),
        txn.getTransactionReference());

    return new ChatResponse(reply, false);
  }
}