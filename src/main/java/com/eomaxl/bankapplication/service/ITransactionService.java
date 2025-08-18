package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Transaction;
import com.eomaxl.bankapplication.domain.model.TransactionStatus;
import com.eomaxl.bankapplication.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Transaction management operations
 * Provides contract for transaction-related business logic and audit trail
 */
public interface ITransactionService {

    /**
     * Creates a new transaction record
     * @param transaction Transaction entity to create
     * @return Created transaction with generated transaction ID
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account not found
     */
    Transaction createTransaction(Transaction transaction);

    /**
     * Processes a deposit transaction
     * @param accountNumber Account number to deposit to
     * @param amount Amount to deposit
     * @param description Transaction description
     * @return Created transaction record
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if amount invalid
     */
    Transaction deposit(String accountNumber, BigDecimal amount, String description);

    /**
     * Processes a withdrawal transaction
     * @param accountNumber Account number to withdraw from
     * @param amount Amount to withdraw
     * @param description Transaction description
     * @return Created transaction record
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     * @throws com.eomaxl.bankapplication.domain.exception.InsufficientFundsException if insufficient balance
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if amount invalid
     */
    Transaction withdrawal(String accountNumber, BigDecimal amount, String description);

    /**
     * Processes a transfer transaction (creates two transaction records)
     * @param fromAccountNumber Source account number
     * @param toAccountNumber Destination account number
     * @param amount Amount to transfer
     * @param description Transfer description
     * @return List containing both transaction records (outgoing and incoming)
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if either account not found
     * @throws com.eomaxl.bankapplication.domain.exception.InsufficientFundsException if insufficient balance
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if transfer invalid
     */
    List<Transaction> transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description);

    /**
     * Finds a transaction by its unique ID
     * @param id Transaction ID
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findById(Long id);

    /**
     * Finds a transaction by its transaction ID
     * @param transactionId Transaction ID (e.g., "TXN001")
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findByTransactionId(String transactionId);

    /**
     * Finds transactions for an account with pagination
     * @param accountId Account ID
     * @param pageable Pagination parameters
     * @return Page of transactions for the account
     */
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    /**
     * Finds transactions for an account by account number with pagination
     * @param accountNumber Account number
     * @param pageable Pagination parameters
     * @return Page of transactions for the account
     */
    Page<Transaction> findByAccountNumber(String accountNumber, Pageable pageable);

    /**
     * Finds transactions for a customer with pagination
     * @param customerId Customer ID
     * @param pageable Pagination parameters
     * @return Page of transactions for the customer
     */
    Page<Transaction> findByCustomerId(String customerId, Pageable pageable);

    /**
     * Finds transactions within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions within the date range
     */
    List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds transactions for an account within a date range
     * @param accountId Account ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of transactions for the account within the date range
     */
    List<Transaction> findByAccountAndDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds transactions by transaction type
     * @param transactionType Transaction type
     * @return List of transactions of the specified type
     */
    List<Transaction> findByTransactionType(TransactionType transactionType);

    /**
     * Finds transactions by status
     * @param status Transaction status
     * @return List of transactions with the specified status
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Finds incoming transfer transactions for an account
     * @param accountId Account ID
     * @return List of incoming transfer transactions
     */
    List<Transaction> findIncomingTransfers(Long accountId);

    /**
     * Finds outgoing transfer transactions for an account
     * @param accountId Account ID
     * @return List of outgoing transfer transactions
     */
    List<Transaction> findOutgoingTransfers(Long accountId);

    /**
     * Finds all transfer transactions (both incoming and outgoing) for an account
     * @param accountId Account ID
     * @return List of all transfer transactions
     */
    List<Transaction> findAllTransfers(Long accountId);

    /**
     * Gets the total transaction amount for an account by transaction type
     * @param accountId Account ID
     * @param transactionType Transaction type
     * @return Total amount for completed transactions of the specified type
     */
    BigDecimal getTotalAmountByAccountAndType(Long accountId, TransactionType transactionType);

    /**
     * Counts transactions for an account within a date range
     * @param accountId Account ID
     * @param startDate Start date
     * @param endDate End date
     * @return Number of transactions within the date range
     */
    Long countTransactionsByAccountAndDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Gets daily transaction summary for an account
     * @param accountId Account ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of daily transaction summaries (date, count, total amount)
     */
    List<Object[]> getDailyTransactionSummary(Long accountId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Updates a transaction's status
     * @param transactionId Transaction ID
     * @param status New transaction status
     * @return Updated transaction
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if transaction not found
     */
    Transaction updateTransactionStatus(String transactionId, TransactionStatus status);

    /**
     * Finds pending transactions older than the specified time
     * @param cutoffTime Cutoff time for pending transactions
     * @return List of old pending transactions
     */
    List<Transaction> findPendingTransactionsOlderThan(LocalDateTime cutoffTime);

    /**
     * Cleans up old pending transactions by marking them as failed
     * @param cutoffTime Cutoff time for cleanup
     */
    void cleanupPendingTransactions(LocalDateTime cutoffTime);

    /**
     * Checks if a transaction exists with the given transaction ID
     * @param transactionId Transaction ID to check
     * @return true if transaction exists
     */
    boolean existsByTransactionId(String transactionId);
}
