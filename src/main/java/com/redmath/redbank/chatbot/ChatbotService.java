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
import org.springframework.stereotype.Service;
import java.time.LocalDate;

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
      return new ChatResponse("I couldn't identify your account. Please sign in again and try once more.", false);
    }

    final AccountHolder accountHolder;
    try {
      accountHolder = accountHolderService.getAccountHolderByUserId(userId);
    } catch (RuntimeException e) {
      return new ChatResponse("I couldn't load your account details right now. Please try again later.", false);
    }

    String myAccountNumber = accountHolder.getAccountNumber();
    Long accountHolderId = accountHolder.getId();
    FinancialQueryIntent intent = llmClient.parseIntent(message, LocalDate.now(), String.valueOf(accountHolderId));

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
      return new ChatResponse("I can only answer questions about your own transactions and balance.", false);
    }

    // Resolve counterparty name -> account number, if the question mentioned a person
    if (intent.getCounterpartyName() != null) {
      var resolution = counterpartyResolver.resolve(intent.getCounterpartyName());
      switch (resolution.status) {
        case NOT_FOUND:
          return new ChatResponse("I couldn't find anyone named \"" + intent.getCounterpartyName()
              + "\" in your transaction history.", false);
        case AMBIGUOUS:
          return new ChatResponse("I found multiple people named \"" + intent.getCounterpartyName()
              + "\". Could you give me more detail (e.g. full name)?", true);
        case RESOLVED:
          intent.setCounterpartyAccountNumber(resolution.accountNumber);
      }
    }

    return switch (intent.getQueryType()) {
      case TRANSACTION_AGGREGATE -> handleTransactionAggregate(intent, myAccountNumber);
      case BALANCE_AT_DATE -> handleBalanceAtDate(intent, accountHolderId);
      case PROJECTION -> handleProjection(accountHolderId);
      default -> new ChatResponse("Sorry, I couldn't process that question.", false);
    };
  }

  private ChatResponse handleTransactionAggregate(FinancialQueryIntent intent, String myAccountNumber) {
    var result = transactionQueryService.execute(intent, myAccountNumber);

    String categoryPart = intent.getCategory() != null ? " on " + intent.getCategory() : "";
    String periodPart = (intent.getStartDate() != null && intent.getEndDate() != null)
        ? " between " + intent.getStartDate() + " and " + intent.getEndDate() : "";

    String reply = switch (intent.getDirection()) {
      case DEBIT -> "You spent $" + result.sum() + categoryPart + periodPart
          + " across " + result.count() + " transaction(s).";
      case CREDIT -> "You received $" + result.sum() + categoryPart + periodPart
          + " across " + result.count() + " transaction(s).";
      default -> "Total movement" + categoryPart + periodPart + ": $" + result.sum()
          + " across " + result.count() + " transaction(s).";
    };

    return new ChatResponse(reply, false);
  }

  private ChatResponse handleBalanceAtDate(FinancialQueryIntent intent, Long accountHolderId) {
    return balanceQueryService.getBalanceAsOf(accountHolderId, intent.getAsOfDate())
        .map(balance -> new ChatResponse(
            "Your balance on " + intent.getAsOfDate() + " was $" + balance + ".", false))
        .orElse(new ChatResponse(
            "I don't have a balance record on or before " + intent.getAsOfDate() + ".", false));
  }

  private ChatResponse handleProjection(Long accountHolderId) {
    var projected = balanceQueryService.projectMonthEndBalance(accountHolderId);
    return new ChatResponse(
        "Based on your spending pattern over the last 30 days, your projected balance at month-end " +
            "is approximately $" + projected + ". This is an estimate, not a guarantee.", false);
  }
}