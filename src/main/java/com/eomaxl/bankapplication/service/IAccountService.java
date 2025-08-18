package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.AccountStatus;
import com.eomaxl.bankapplication.domain.model.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Account management operations
 * Provides contract for account-related business logic with thread-safe operations
 */
public interface IAccountService {
    /**
     * Creates a new account in the system
     * @param account Account entity to create
     * @return Created account with generated account number
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if bank or account holder not found
     */
    Account createAccount(Account account);

    /**
     * Finds an account by its unique ID
     * @param id Account ID
     * @return Optional containing account if found
     */
    Optional<Account> findById(Long id);

    /**
     * Finds an account by its account number
     * @param accountNumber Account number
     * @return Optional containing account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Gets an account by account number or throws exception if not found
     * @param accountNumber Account number
     * @return Account entity
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     */
    Account getAccountByNumber(String accountNumber);

    /**
     * Finds all accounts for a specific account holder
     * @param accountHolderId Account holder ID
     * @return List of accounts
     */
    List<Account> findByAccountHolderId(Long accountHolderId);

    /**
     * Finds all accounts for a specific customer
     * @param customerId Customer ID
     * @return List of accounts
     */
    List<Account> findByCustomerId(String customerId);

    /**
     * Finds all accounts for a specific bank
     * @param bankId Bank ID
     * @return List of accounts
     */
    List<Account> findByBankId(Long bankId);

    /**
     * Gets the current balance of an account (cached for performance)
     * @param accountNumber Account number
     * @return Current account balance
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if acc   ount not found
     */
    BigDecimal getBalance(String accountNumber);

    /**
     * Credits money to an account (thread-safe operation)
     * @param accountNumber Account number
     * @param amount Amount to credit (must be positive)
     * @param description Transaction description
     * @return Updated account with new balance
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if amount invalid or account inactive
     */
    Account credit(String accountNumber, BigDecimal amount, String description);

    /**
     * Debits money from an account (thread-safe operation)
     * @param accountNumber Account number
     * @param amount Amount to debit (must be positive)
     * @param description Transaction description
     * @return Updated account with new balance
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     * @throws com.eomaxl.bankapplication.domain.exception.InsufficientFundsException if insufficient balance
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if amount invalid or account inactive
     */
    Account debit(String accountNumber, BigDecimal amount, String description);

    /**
     * Transfers money between two accounts (atomic operation)
     * @param fromAccountNumber Source account number
     * @param toAccountNumber Destination account number
     * @param amount Amount to transfer
     * @param description Transfer description
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if either account not found
     * @throws com.eomaxl.bankapplication.domain.exception.InsufficientFundsException if insufficient balance
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if transfer invalid
     */
    void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description);

    /**
     * Updates an account's status
     * @param id Account ID
     * @param status New account status
     * @return Updated account
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account not found
     */
    Account updateAccountStatus(Long id, AccountStatus status);

    /**
     * Finds accounts within a balance range
     * @param minBalance Minimum balance
     * @param maxBalance Maximum balance
     * @return List of accounts within the range
     */
    List<Account> findByBalanceRange(BigDecimal minBalance, BigDecimal maxBalance);

    /**
     * Finds accounts by account type
     * @param accountType Account type
     * @return List of accounts of the specified type
     */
    List<Account> findByAccountType(AccountType accountType);

    /**
     * Finds accounts by status
     * @param status Account status
     * @return List of accounts with the specified status
     */
    List<Account> findByStatus(AccountStatus status);

    /**
     * Finds active accounts for an account holder with pagination
     * @param accountHolderId Account holder ID
     * @param pageable Pagination parameters
     * @return Page of active accounts
     */
    Page<Account> findActiveAccountsByAccountHolder(Long accountHolderId, Pageable pageable);

    /**
     * Gets the total balance across all accounts for a bank
     * @param bankId Bank ID
     * @return Total balance across all bank accounts
     */
    BigDecimal getTotalBalanceByBankId(Long bankId);

    /**
     * Finds dormant accounts (no transactions since specified date)
     * @param lastTransactionDate Cutoff date for last transaction
     * @return List of dormant accounts
     */
    List<Account> findDormantAccounts(LocalDateTime lastTransactionDate);

    /**
     * Finds high-value accounts above a threshold
     * @param threshold Minimum balance threshold
     * @return List of high-value accounts
     */
    List<Account> findHighValueAccounts(BigDecimal threshold);

    /**
     * Checks if an account exists with the given account number
     * @param accountNumber Account number to check
     * @return true if account exists
     */
    boolean existsByAccountNumber(String accountNumber);
}
