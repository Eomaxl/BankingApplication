package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Account Service Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IBankService bankService;

    @Mock
    private IAccountHolderService accountHolderService;

    @InjectMocks
    private IAccountService accountService;

    private Account testAccount;
    private Bank testBank;
    private AccountHolder testAccountHolder;
    private Person testPerson;

    @BeforeEach
    void setUp() {
        testPerson = Person.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .build();

        testBank = Bank.builder()
                .id(1L)
                .bankName("Test Bank")
                .bankCode("TB001")
                .build();

        testAccountHolder = AccountHolder.builder()
                .id(1L)
                .person(testPerson)
                .customerId("CUST001")
                .status(AccountHolder.AccountHolderStatus.ACTIVE)
                .build();

        testAccount = Account.builder()
                .id(1L)
                .accountNumber("ACC001")
                .balance(new BigDecimal("1000.00"))
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .bank(testBank)
                .accountHolder(testAccountHolder)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccountSuccessfully() {
        // Given
        when(bankService.findById(1L)).thenReturn(Optional.of(testBank));
        when(accountHolderService.findById(1L)).thenReturn(Optional.of(testAccountHolder));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.createAccount(testAccount);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.ACTIVE);

        verify(bankService).findById(1L);
        verify(accountHolderService).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("Should find account by account number")
    void shouldFindAccountByAccountNumber() {
        // Given
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        Optional<Account> result = accountService.findByAccountNumber("ACC001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo("ACC001");

        verify(accountRepository).findByAccountNumber("ACC001");
    }

    @Test
    @DisplayName("Should get account by number or throw exception")
    void shouldGetAccountByNumberOrThrowException() {
        // Given
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        Account result = accountService.getAccountByNumber("ACC001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account not found")
    void shouldThrowAccountNotFoundExceptionWhenAccountNotFound() {
        // Given
        when(accountRepository.findByAccountNumber("INVALID")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountByNumber("INVALID"))
                .isInstanceOf(com.eomaxl.bankapplication.domain.exception.AccountNotFoundException.class);
    }

    @Test
    @DisplayName("Should get account balance")
    void shouldGetAccountBalance() {
        // Given
        when(accountRepository.findByAccountNumber("ACC001")).thenReturn(Optional.of(testAccount));

        // When
        BigDecimal balance = accountService.getBalance("ACC001");

        // Then
        assertThat(balance).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should credit account successfully")
    void shouldCreditAccountSuccessfully() {
        // Given
        BigDecimal creditAmount = new BigDecimal("500.00");
        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.credit("ACC001", creditAmount, "Test credit");

        // Then
        assertThat(result).isNotNull();
        verify(accountRepository).findByAccountNumberWithLock("ACC001");
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Should debit account successfully")
    void shouldDebitAccountSuccessfully() {
        // Given
        BigDecimal debitAmount = new BigDecimal("200.00");
        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.debit("ACC001", debitAmount, "Test debit");

        // Then
        assertThat(result).isNotNull();
        verify(accountRepository).findByAccountNumberWithLock("ACC001");
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Should throw exception when debiting more than balance")
    void shouldThrowExceptionWhenDebitingMoreThanBalance() {
        // Given
        BigDecimal debitAmount = new BigDecimal("2000.00"); // More than balance
        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(testAccount));

        // When & Then
        assertThatThrownBy(() -> accountService.debit("ACC001", debitAmount, "Test debit"))
                .isInstanceOf(com.eomaxl.bankapplication.domain.exception.InsufficientFundsException.class);
    }

    @Test
    @DisplayName("Should transfer money between accounts successfully")
    void shouldTransferMoneyBetweenAccountsSuccessfully() {
        // Given
        Account toAccount = Account.builder()
                .id(2L)
                .accountNumber("ACC002")
                .balance(new BigDecimal("500.00"))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .bank(testBank)
                .accountHolder(testAccountHolder)
                .build();

        BigDecimal transferAmount = new BigDecimal("300.00");

        when(accountRepository.findByAccountNumberWithLock("ACC001")).thenReturn(Optional.of(testAccount));
        when(accountRepository.findByAccountNumberWithLock("ACC002")).thenReturn(Optional.of(toAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount, toAccount);

        // When
        accountService.transfer("ACC001", "ACC002", transferAmount, "Test transfer");

        // Then
        verify(accountRepository).findByAccountNumberWithLock("ACC001");
        verify(accountRepository).findByAccountNumberWithLock("ACC002");
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when transferring to same account")
    void shouldThrowExceptionWhenTransferringToSameAccount() {
        // Given
        BigDecimal transferAmount = new BigDecimal("300.00");

        // When & Then
        assertThatThrownBy(() -> accountService.transfer("ACC001", "ACC001", transferAmount, "Test transfer"))
                .isInstanceOf(com.eomaxl.bankapplication.domain.exception.BankingException.class)
                .hasMessageContaining("Cannot transfer to the same account");
    }

    @Test
    @DisplayName("Should update account status successfully")
    void shouldUpdateAccountStatusSuccessfully() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        Account result = accountService.updateAccountStatus(1L, AccountStatus.SUSPENDED);

        // Then
        assertThat(result).isNotNull();
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(testAccount);
    }

    @Test
    @DisplayName("Should check if account exists by account number")
    void shouldCheckIfAccountExistsByAccountNumber() {
        // Given
        when(accountRepository.existsByAccountNumber("ACC001")).thenReturn(true);

        // When
        boolean exists = accountService.existsByAccountNumber("ACC001");

        // Then
        assertThat(exists).isTrue();
        verify(accountRepository).existsByAccountNumber("ACC001");
    }
}
