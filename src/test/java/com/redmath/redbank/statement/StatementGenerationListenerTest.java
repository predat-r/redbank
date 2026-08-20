package com.redmath.redbank.statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redmath.redbank.account.AccountHolder;
import com.redmath.redbank.account.AccountHolderRepository;
import com.redmath.redbank.balance.BalanceRepository;
import com.redmath.redbank.user.User;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatementGenerationListenerTest {

  @Mock
  private AccountHolderRepository accountHolderRepository;

  @Mock
  private BalanceRepository balanceRepository;

  @Mock
  private com.redmath.redbank.transaction.BankTransactionRepository bankTransactionRepository;

  @Mock
  private StatementPdfGenerator pdfGenerator;

  @Mock
  private StatementEmailService emailService;

  private StatementGenerationListener listener;

  @BeforeEach
  void setUp() {
    listener = new StatementGenerationListener(accountHolderRepository, balanceRepository, bankTransactionRepository, pdfGenerator, emailService);
  }

  @Test
  void shouldGenerateAndSendStatement() {
    AccountHolder accountHolder = new AccountHolder();
    accountHolder.setId(10L);
    accountHolder.setAccountNumber("12345");
    
    User user = User.builder()
      .name("John Doe")
      .address("123 Main St")
      .build();
    accountHolder.setUser(user);

    when(accountHolderRepository.findById(10L)).thenReturn(Optional.of(accountHolder));
    when(balanceRepository.findTopByAccountHolderIdAndEntryDateLessThanOrderByEntryDateDescIdDesc(eq(10L), any()))
        .thenReturn(Optional.empty());
    when(balanceRepository.findTopByAccountHolderIdAndEntryDateLessThanEqualOrderByEntryDateDescIdDesc(eq(10L), any()))
        .thenReturn(Optional.empty());
    when(balanceRepository.findByAccountHolderIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(eq(10L), any(), any()))
        .thenReturn(Collections.emptyList());

    byte[] pdfBytes = new byte[]{1, 2, 3};
    when(pdfGenerator.generatePdf(any())).thenReturn(pdfBytes);

    StatementRequestedEvent event = new StatementRequestedEvent(10L, LocalDate.now().minusDays(1), LocalDate.now());
    listener.handleStatementRequested(event);

    verify(pdfGenerator).generatePdf(any());
    verify(emailService).sendStatement(eq(accountHolder), eq(pdfBytes), eq(event.fromDate()), eq(event.toDate()));
  }
}
