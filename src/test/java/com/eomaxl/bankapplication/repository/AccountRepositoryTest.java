package com.eomaxl.bankapplication.repository;

import com.eomaxl.bankapplication.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

DataJpaTest
@ActiveProfiles("test")
@DisplayName("Account Repository Tests")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;
    private Bank testBank;
    private Person testPerson;
    private AccountHolder testAccountHolder;

    @BeforeEach
    void setUp() {
        // Create and persist test data
        testPerson = Person.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phoneNumber("+1-555-1001")
                .address("123 Main St")
                .dateOfBirth(LocalDateTime.of(1990, 1, 1, 0, 0))
                .build();
        entityManager.persistAndFlush(testPerson);

        testBank = Bank.builder()
                .bankName("Test Bank")
                .bankCode("TB001")
                .address("456 Bank St")
                .phoneNumber("+1-555-2001")
                .email("info@testbank.com")
                .build();
        entityManager.persistAndFlush(testBank);

        testAccountHolder = AccountHolder.builder()
                .person(testPerson)
                .customerId("CUST001")
                .status(AccountHolder.AccountHolderStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(testAccountHolder);

        testAccount = Account.builder()
                .accountNumber("ACC001")
                .balance(new BigDecimal("1000.00"))
                .accountType(AccountType.SAVINGS)
                .status(AccountStatus.ACTIVE)
                .bank(testBank)
                .accountHolder(testAccountHolder)
                .build();
        entityManager.persistAndFlush(testAccount);
    }

    @Test
    @DisplayName("Should find account by account number")
    void shouldFindAccountByAccountNumber() {
        // When
        Optional<Account> result = accountRepository.findByAccountNumber("ACC001");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAccountNumber()).isEqualTo("ACC001");
        assertThat(result.get().getBalance()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should check if account exists by account number")
    void shouldCheckIfAccountExistsByAccountNumber() {
        // When
        boolean exists = accountRepository.existsByAccountNumber("ACC001");
        boolean notExists = accountRepository.existsByAccountNumber("INVALID");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find accounts by account holder ID")
    void shouldFindAccountsByAccountHolderId() {
        // When
        List<Account> accounts = accountRepository.findByAccountHolderId(testAccountHolder.getId());

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("Should find accounts by bank ID")
    void shouldFindAccountsByBankId() {
        // When
        List<Account> accounts = accountRepository.findByBankId(testBank.getId());

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("Should find accounts by account type")
    void shouldFindAccountsByAccountType() {
        // When
        List<Account> savingsAccounts = accountRepository.findByAccountType(AccountType.SAVINGS);
        List<Account> checkingAccounts = accountRepository.findByAccountType(AccountType.CHECKING);

        // Then
        assertThat(savingsAccounts).hasSize(1);
        assertThat(checkingAccounts).isEmpty();
    }

    @Test
    @DisplayName("Should find accounts by status")
    void shouldFindAccountsByStatus() {
        // When
        List<Account> activeAccounts = accountRepository.findByStatus(AccountStatus.ACTIVE);
        List<Account> inactiveAccounts = accountRepository.findByStatus(AccountStatus.INACTIVE);

        // Then
        assertThat(activeAccounts).hasSize(1);
        assertThat(inactiveAccounts).isEmpty();
    }

    @Test
    @DisplayName("Should find accounts by balance range")
    void shouldFindAccountsByBalanceRange() {
        // When
        List<Account> accounts = accountRepository.findByBalanceBetween(
                new BigDecimal("500.00"),
                new BigDecimal("1500.00")
        );

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getBalance()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should find accounts by customer ID")
    void shouldFindAccountsByCustomerId() {
        // When
        List<Account> accounts = accountRepository.findByCustomerId("CUST001");

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountHolder().getCustomerId()).isEqualTo("CUST001");
    }

    @Test
    @DisplayName("Should get total balance by bank ID")
    void shouldGetTotalBalanceByBankId() {
        // Given - Create another account for the same bank
        Account secondAccount = Account.builder()
                .accountNumber("ACC002")
                .balance(new BigDecimal("500.00"))
                .accountType(AccountType.CHECKING)
                .status(AccountStatus.ACTIVE)
                .bank(testBank)
                .accountHolder(testAccountHolder)
                .build();
        entityManager.persistAndFlush(secondAccount);

        // When
        BigDecimal totalBalance = accountRepository.getTotalBalanceByBankId(testBank.getId());

        // Then
        assertThat(totalBalance).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    @DisplayName("Should count active accounts by type")
    void shouldCountActiveAccountsByType() {
        // When
        Long savingsCount = accountRepository.countActiveAccountsByType(AccountType.SAVINGS);
        Long checkingCount = accountRepository.countActiveAccountsByType(AccountType.CHECKING);

        // Then
        assertThat(savingsCount).isEqualTo(1L);
        assertThat(checkingCount).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should update account status")
    void shouldUpdateAccountStatus() {
        // When
        int updatedRows = accountRepository.updateAccountStatus(testAccount.getId(), AccountStatus.SUSPENDED);

        // Then
        assertThat(updatedRows).isEqualTo(1);

        // Verify the update
        entityManager.clear(); // Clear persistence context to force reload
        Account updatedAccount = entityManager.find(Account.class, testAccount.getId());
        assertThat(updatedAccount.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should find accounts created between dates")
    void shouldFindAccountsCreatedBetweenDates() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // When
        List<Account> accounts = accountRepository.findAccountsCreatedBetween(startDate, endDate);

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountNumber()).isEqualTo("ACC001");
    }

    @Test
    @DisplayName("Should find accounts by account holder email")
    void shouldFindAccountsByAccountHolderEmail() {
        // When
        List<Account> accounts = accountRepository.findByAccountHolderEmail("john.doe@email.com");

        // Then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getAccountHolder().getPerson().getEmail()).isEqualTo("john.doe@email.com");
    }
}
