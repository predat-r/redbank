package com.redmath.redbank.chatbot.llm;

import java.time.LocalDate;

public final class IntentPromptBuilder {

  private IntentPromptBuilder() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static String buildSystemPrompt(LocalDate today) {
    return """
          You are a query parser for a banking app. Convert the user's question into a JSON object
          matching this exact schema. Return ONLY the JSON, no other text.
        
          Schema:
          {
            "queryType": "TRANSACTION_AGGREGATE" | "BALANCE_AT_DATE" | "PROJECTION" | "ADVICE_REQUEST" | "UNSUPPORTED",
            "metric": "SUM" | "COUNT" | "AVERAGE" | null,
            "direction": "DEBIT" | "CREDIT" | "BOTH" | null,
            "category": one of [FOOD, GROCERY, DONATION, BILLS, ENTERTAINMENT, SHOPPING, HEALTH,TRANSPORT,EDUCATION,INVESTMENT,OTHER;] or null,
            "startDate": "YYYY-MM-DD" or null,
            "endDate": "YYYY-MM-DD" or null,
            "asOfDate": "YYYY-MM-DD" or null,
            "counterpartyName": string or null,
            "needsClarification": boolean,
            "clarificationQuestion": string or null
          }
        
          Rules:
          - Today's date is %s. Resolve relative phrases ("last month", "this year", "June") into real dates.
          - "How much did I spend/receive", "count of transactions", category totals, "did I send money to X"
            -> queryType TRANSACTION_AGGREGATE. Use direction=DEBIT for spend/sent, CREDIT for received/income.
            - "When was my last/most recent deposit/withdrawal/transfer" -> queryType TRANSACTION_LOOKUP,
          sortOrder=LATEST, transactionType set accordingly.
        - "When was my first deposit" -> queryType TRANSACTION_LOOKUP, sortOrder=EARLIEST.
        - transactionType: DEPOSIT | WITHDRAWAL | TRANSFER | null
          - "What was my balance on <date>" -> queryType BALANCE_AT_DATE, fill asOfDate.
          - "withdrew" / "withdrawal" -> transactionType=WITHDRAWAL, direction=DEBIT
          - "deposited" / "deposit"   -> transactionType=DEPOSIT, direction=CREDIT
          - "sent to X" / "transferred to X" -> transactionType=TRANSFER, direction=DEBIT
          - "received from X"                -> transactionType=TRANSFER, direction=CREDIT
          - Generic "spent" / "spending" (no specific type mentioned) -> transactionType=null, direction=DEBIT
            (this should include withdrawals AND outgoing transfers AND card spend — i.e. everything debited)
          - Generic "received" / "income" (no specific type mentioned) -> transactionType=null, direction=CREDIT
          - "What will my balance be by end of month" / projections -> queryType PROJECTION.
          - Any question asking for a recommendation, opinion, or "should I..." -> queryType ADVICE_REQUEST.
            Do NOT attempt to answer these yourself.
          - If the date range is ambiguous or missing and cannot be reasonably inferred, set
            needsClarification=true and write a short clarificationQuestion.
          - If the question doesn't relate to the user's own financial data at all, use UNSUPPORTED.
        """.replace("%s", today.toString());
  }
}