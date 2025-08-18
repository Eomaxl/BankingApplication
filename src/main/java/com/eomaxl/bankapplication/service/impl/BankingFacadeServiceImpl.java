package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Facade Service that provides high-level banking operations
 * Orchestrates multiple services to provide complex business functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BankingFacadeServiceImpl implements IBankingFacadeService {
    private final IPersonService personService;
    private final IBankService bankService;
    private final IAccountHolderService accountHolderService;
    private final IAccountService accountService;
    private final ITransactionService transactionService;

    /**
     * Complete customer onboarding process
     */
    @Transactional
    public Account onboardCustomer(Person person, String bankCode, AccountType accountType,
                                   BigDecimal initialDeposit) {
        log.info("Starting customer onboarding for: {}", person.getEmail());

        try {
            // Find bank
            Bank bank = bankService.findByBankCode(bankCode)
                    .orElseThrow(() -> new BankingException("Bank not found with code: " + bankCode, "BANK_NOT_FOUND"));

            // Create or get person
            Person savedPerson;
            Optional<Person> existingPerson = personService.findByEmail(person.getEmail());
            if (existingPerson.isPresent()) {
                savedPerson = existingPerson.get();
                log.info("Using existing person: {}", savedPerson.getEmail());
            } else {
                savedPerson = personService.createPerson(person);
                log.info("Created new person: {}", savedPerson.getEmail());
            }

            // Create account holder
            AccountHolder accountHolder = accountHolderService.createAccountHolder(savedPerson);
            log.info("Created account holder with customer ID: {}", accountHolder.getCustomerId());

            // Create account
            Account account = Account.builder()
                    .accountType(accountType)
                    .status(AccountStatus.ACTIVE)
                    .balance(BigDecimal.ZERO)
                    .bank(bank)
                    .accountHolder(accountHolder)
                    .build();

            Account savedAccount = accountService.createAccount(account);
            log.info("Created account with number: {}", savedAccount.getAccountNumber());

            // Make initial deposit if provided
            if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                transactionService.deposit(savedAccount.getAccountNumber(), initialDeposit, "Initial deposit");
                log.info("Made initial deposit of {} to account: {}", initialDeposit, savedAccount.getAccountNumber());
            }

            log.info("Successfully completed customer onboarding for: {}", person.getEmail());
            return accountService.findByAccountNumber(savedAccount.getAccountNumber()).orElse(savedAccount);

        } catch (Exception e) {
            log.error("Failed to onboard customer: {}", person.getEmail(), e);
            throw new BankingException("Customer onboarding failed: " + e.getMessage(), "ONBOARDING_FAILED");
        }
    }

    /**
     * Get complete customer profile with all accounts and recent transactions
     */
    public CustomerProfile getCustomerProfile(String customerId) {
        log.info("Retrieving customer profile for: {}", customerId);

        AccountHolder accountHolder = accountHolderService.findByCustomerId(customerId)
                .orElseThrow(() -> new BankingException("Customer not found: " + customerId, "CUSTOMER_NOT_FOUND"));

        List<Account> accounts = accountService.findByCustomerId(customerId);

        return CustomerProfile.builder()
                .accountHolder(accountHolder)
                .accounts(accounts)
                .totalBalance(calculateTotalBalance(accounts))
                .build();
    }

    /**
     * Perform money transfer with complete transaction logging
     */
    @Transactional
    public TransferResult performTransfer(String fromAccountNumber, String toAccountNumber,
                                          BigDecimal amount, String description) {
        log.info("Performing transfer: {} from {} to {}", amount, fromAccountNumber, toAccountNumber);

        try {
            // Validate accounts exist and are active
            Account fromAccount = accountService.getAccountByNumber(fromAccountNumber);
            Account toAccount = accountService.getAccountByNumber(toAccountNumber);

            BigDecimal fromBalanceBefore = fromAccount.getBalance();
            BigDecimal toBalanceBefore = toAccount.getBalance();

            // Perform transfer and create transaction records
            List<Transaction> transactions = transactionService.transfer(fromAccountNumber, toAccountNumber, amount, description);

            // Get updated balances
            Account updatedFromAccount = accountService.getAccountByNumber(fromAccountNumber);
            Account updatedToAccount = accountService.getAccountByNumber(toAccountNumber);

            TransferResult result = TransferResult.builder()
                    .success(true)
                    .fromAccountNumber(fromAccountNumber)
                    .toAccountNumber(toAccountNumber)
                    .amount(amount)
                    .fromBalanceBefore(fromBalanceBefore)
                    .fromBalanceAfter(updatedFromAccount.getBalance())
                    .toBalanceBefore(toBalanceBefore)
                    .toBalanceAfter(updatedToAccount.getBalance())
                    .transactions(transactions)
                    .transferDate(LocalDateTime.now())
                    .build();

            log.info("Successfully completed transfer: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Transfer failed: {} from {} to {}", amount, fromAccountNumber, toAccountNumber, e);

            return TransferResult.builder()
                    .success(false)
                    .fromAccountNumber(fromAccountNumber)
                    .toAccountNumber(toAccountNumber)
                    .amount(amount)
                    .errorMessage(e.getMessage())
                    .transferDate(LocalDateTime.now())
                    .build();
        }
    }

    /**
     * Get account statement with transactions for a date range
     */
    public AccountStatement getAccountStatement(String accountNumber, LocalDateTime startDate,
                                                LocalDateTime endDate, Pageable pageable) {
        log.info("Generating account statement for {} from {} to {}", accountNumber, startDate, endDate);

        Account account = accountService.getAccountByNumber(accountNumber);
        Page<Transaction> transactions = transactionService.findByAccountNumber(accountNumber, pageable);

        List<Transaction> dateRangeTransactions = transactionService.findByAccountAndDateRange(
                account.getId(), startDate, endDate);

        BigDecimal totalCredits = dateRangeTransactions.stream()
                .filter(Transaction::isCreditTransaction)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebits = dateRangeTransactions.stream()
                .filter(Transaction::isDebitTransaction)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AccountStatement.builder()
                .account(account)
                .transactions(transactions)
                .startDate(startDate)
                .endDate(endDate)
                .totalCredits(totalCredits)
                .totalDebits(totalDebits)
                .netAmount(totalCredits.subtract(totalDebits))
                .transactionCount((long) dateRangeTransactions.size())
                .build();
    }

    /**
     * Close account with all validations
     */
    @Transactional
    public void closeAccount(String accountNumber, String reason) {
        log.info("Closing account: {} with reason: {}", accountNumber, reason);

        Account account = accountService.getAccountByNumber(accountNumber);

        // Check if account has zero balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BankingException("Cannot close account with non-zero balance: " + account.getBalance(),
                    "NON_ZERO_BALANCE");
        }

        // Update account status
        accountService.updateAccountStatus(account.getId(), AccountStatus.CLOSED);

        // Create closure transaction record
        Transaction closureTransaction = Transaction.builder()
                .amount(BigDecimal.ZERO)
                .transactionType(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .description("Account closure: " + reason)
                .account(account)
                .balanceBefore(BigDecimal.ZERO)
                .balanceAfter(BigDecimal.ZERO)
                .transactionDate(LocalDateTime.now())
                .build();

        transactionService.createTransaction(closureTransaction);

        log.info("Successfully closed account: {}", accountNumber);
    }

    /**
     * Get bank summary with all statistics
     */
    public BankSummary getBankSummary(String bankCode) {
        log.info("Generating bank summary for: {}", bankCode);

        Bank bank = bankService.findByBankCode(bankCode)
                .orElseThrow(() -> new BankingException("Bank not found: " + bankCode, "BANK_NOT_FOUND"));

        List<Account> accounts = accountService.findByBankId(bank.getId());
        BigDecimal totalBalance = accountService.getTotalBalanceByBankId(bank.getId());

        long activeAccounts = accounts.stream()
                .mapToLong(account -> account.getStatus() == AccountStatus.ACTIVE ? 1 : 0)
                .sum();

        return BankSummary.builder()
                .bank(bank)
                .totalAccounts((long) accounts.size())
                .activeAccounts(activeAccounts)
                .totalBalance(totalBalance)
                .averageBalance(accounts.isEmpty() ? BigDecimal.ZERO :
                        totalBalance.divide(BigDecimal.valueOf(accounts.size()), 2, BigDecimal.ROUND_HALF_UP))
                .build();
    }

    private BigDecimal calculateTotalBalance(List<Account> accounts) {
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Inner classes for complex return types
    @lombok.Data
    @lombok.Builder
    public static class CustomerProfile {
        private AccountHolder accountHolder;
        private List<Account> accounts;
        private BigDecimal totalBalance;
    }

    @lombok.Data
    @lombok.Builder
    public static class TransferResult {
        private boolean success;
        private String fromAccountNumber;
        private String toAccountNumber;
        private BigDecimal amount;
        private BigDecimal fromBalanceBefore;
        private BigDecimal fromBalanceAfter;
        private BigDecimal toBalanceBefore;
        private BigDecimal toBalanceAfter;
        private List<Transaction> transactions;
        private LocalDateTime transferDate;
        private String errorMessage;
    }

    @lombok.Data
    @lombok.Builder
    public static class AccountStatement {
        private Account account;
        private Page<Transaction> transactions;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal totalCredits;
        private BigDecimal totalDebits;
        private BigDecimal netAmount;
        private Long transactionCount;
    }

    @lombok.Data
    @lombok.Builder
    public static class BankSummary {
        private Bank bank;
        private Long totalAccounts;
        private Long activeAccounts;
        private BigDecimal totalBalance;
        private BigDecimal averageBalance;
    }
}
