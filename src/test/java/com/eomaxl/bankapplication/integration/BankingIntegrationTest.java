package com.eomaxl.bankapplication.integration;

import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.service.impl.BankingFacadeServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureWebMvc
@Transactional
@DisplayName("Banking Integration Tests")
class BankingIntegrationTest {

    @Autowired
    private BankingFacadeServiceImpl bankingFacadeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Person testPerson;
    private Bank testBank;

    @BeforeEach
    void setUp() {
        testPerson = Person.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@email.com")
                .phoneNumber("+1-555-1001")
                .address("123 Main St")
                .dateOfBirth(LocalDateTime.of(1990, 1, 1, 0, 0))
                .build();

        testBank = Bank.builder()
                .bankName("Test Bank")
                .bankCode("TB001")
                .address("456 Bank St")
                .phoneNumber("+1-555-2001")
                .email("info@testbank.com")
                .build();
    }

    @Test
    @DisplayName("Should complete full customer onboarding process")
    void shouldCompleteFullCustomerOnboardingProcess() {
        // When
        Account account = bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001", // Using bank from data.sql
                AccountType.SAVINGS,
                new BigDecimal("1000.00")
        );

        // Then
        assertThat(account).isNotNull();
        assertThat(account.getAccountNumber()).isNotNull();
        assertThat(account.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(account.getAccountHolder()).isNotNull();
        assertThat(account.getAccountHolder().getPerson().getEmail()).isEqualTo("john.doe@email.com");
    }

    @Test
    @DisplayName("Should perform money transfer between accounts")
    void shouldPerformMoneyTransferBetweenAccounts() {
        // Given - Create two accounts
        Account fromAccount = bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001",
                AccountType.CHECKING,
                new BigDecimal("2000.00")
        );

        Person toPerson = Person.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@email.com")
                .phoneNumber("+1-555-1002")
                .build();

        Account toAccount = bankingFacadeService.onboardCustomer(
                toPerson,
                "FNB001",
                AccountType.SAVINGS,
                new BigDecimal("500.00")
        );

        // When
        BankingFacadeServiceImpl.TransferResult result = bankingFacadeService.performTransfer(
                fromAccount.getAccountNumber(),
                toAccount.getAccountNumber(),
                new BigDecimal("300.00"),
                "Test transfer"
        );

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("300.00"));
        assertThat(result.getFromBalanceAfter()).isEqualTo(new BigDecimal("1700.00"));
        assertThat(result.getToBalanceAfter()).isEqualTo(new BigDecimal("800.00"));
        assertThat(result.getTransactions()).hasSize(2);
    }

    @Test
    @DisplayName("Should retrieve customer profile with all accounts")
    void shouldRetrieveCustomerProfileWithAllAccounts() {
        // Given
        Account account = bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001",
                AccountType.SAVINGS,
                new BigDecimal("1500.00")
        );

        String customerId = account.getAccountHolder().getCustomerId();

        // When
        BankingFacadeServiceImpl.CustomerProfile profile = bankingFacadeService.getCustomerProfile(customerId);

        // Then
        assertThat(profile).isNotNull();
        assertThat(profile.getAccountHolder().getCustomerId()).isEqualTo(customerId);
        assertThat(profile.getAccounts()).hasSize(1);
        assertThat(profile.getTotalBalance()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(profile.getAccountHolder().getPerson().getEmail()).isEqualTo("john.doe@email.com");
    }

    @Test
    @DisplayName("Should generate bank summary with statistics")
    void shouldGenerateBankSummaryWithStatistics() {
        // Given - Create multiple accounts
        bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001",
                AccountType.SAVINGS,
                new BigDecimal("1000.00")
        );

        Person person2 = Person.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@email.com")
                .build();

        bankingFacadeService.onboardCustomer(
                person2,
                "FNB001",
                AccountType.CHECKING,
                new BigDecimal("2000.00")
        );

        // When
        BankingFacadeServiceImpl.BankSummary summary = bankingFacadeService.getBankSummary("FNB001");

        // Then
        assertThat(summary).isNotNull();
        assertThat(summary.getBank().getBankCode()).isEqualTo("FNB001");
        assertThat(summary.getTotalAccounts()).isGreaterThanOrEqualTo(2L);
        assertThat(summary.getActiveAccounts()).isGreaterThanOrEqualTo(2L);
        assertThat(summary.getTotalBalance()).isGreaterThanOrEqualTo(new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("Should close account with zero balance")
    void shouldCloseAccountWithZeroBalance() {
        // Given
        Account account = bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001",
                AccountType.SAVINGS,
                BigDecimal.ZERO // Zero initial deposit
        );

        // When & Then - Should not throw exception
        assertThatCode(() -> bankingFacadeService.closeAccount(account.getAccountNumber(), "Customer request"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle transfer failure gracefully")
    void shouldHandleTransferFailureGracefully() {
        // Given
        Account account = bankingFacadeService.onboardCustomer(
                testPerson,
                "FNB001",
                AccountType.SAVINGS,
                new BigDecimal("100.00")
        );

        // When - Try to transfer more than balance
        BankingFacadeServiceImpl.TransferResult result = bankingFacadeService.performTransfer(
                account.getAccountNumber(),
                "INVALID_ACCOUNT",
                new BigDecimal("200.00"),
                "Test transfer"
        );

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }
}
