package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.Transaction;
import com.eomaxl.bankapplication.domain.model.TransactionStatus;
import com.eomaxl.bankapplication.domain.model.TransactionType;
import com.eomaxl.bankapplication.repository.TransactionRepository;
import com.eomaxl.bankapplication.service.IAccountService;
import com.eomaxl.bankapplication.service.ITransactionService;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TransactionServiceImpl implements ITransactionService {
    private final TransactionRepository transactionRepository;
    private final IAccountService accountService;

    @Transactional
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating transaction for account: {}", transaction.getAccount().getAccountNumber());

        // Validate account exists
        Account account = accountService.findById(transaction.getAccount().getId())
                .orElseThrow(() -> new BankingException("Account not found with ID: " + transaction.getAccount().getId(),
                        "ACCOUNT_NOT_FOUND"));

        // Generate unique transaction ID
        String transactionId = generateTransactionId();
        while (transactionRepository.existsByTransactionId(transactionId)) {
            transactionId = generateTransactionId();
        }

        transaction.setTransactionId(transactionId);
        transaction.setAccount(account);

        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDateTime.now());
        }

        if (transaction.getStatus() == null) {
            transaction.setStatus(TransactionStatus.PENDING);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Successfully created transaction with ID: {}", savedTransaction.getTransactionId());
        return savedTransaction;
    }

    @Transactional
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        log.info("Processing deposit of {} to account: {}", amount, accountNumber);

        Account account = accountService.getAccountByNumber(accountNumber);
        BigDecimal balanceBefore = account.getBalance();

        // Update account balance
        Account updatedAccount = accountService.credit(accountNumber, amount, description);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .amount(amount)
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .account(updatedAccount)
                .balanceBefore(balanceBefore)
                .balanceAfter(updatedAccount.getBalance())
                .transactionDate(LocalDateTime.now())
                .build();

        return createTransaction(transaction);
    }

    @Transactional
    public Transaction withdrawal(String accountNumber, BigDecimal amount, String description) {
        log.info("Processing withdrawal of {} from account: {}", amount, accountNumber);

        Account account = accountService.getAccountByNumber(accountNumber);
        BigDecimal balanceBefore = account.getBalance();

        // Update account balance
        Account updatedAccount = accountService.debit(accountNumber, amount, description);

        // Create transaction record
        Transaction transaction = Transaction.builder()
                .amount(amount)
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .account(updatedAccount)
                .balanceBefore(balanceBefore)
                .balanceAfter(updatedAccount.getBalance())
                .transactionDate(LocalDateTime.now())
                .build();

        return createTransaction(transaction);
    }

    @Transactional
    public List<Transaction> transfer(String fromAccountNumber, String toAccountNumber,
                                      BigDecimal amount, String description) {
        log.info("Processing transfer of {} from {} to {}", amount, fromAccountNumber, toAccountNumber);

        Account fromAccount = accountService.getAccountByNumber(fromAccountNumber);
        Account toAccount = accountService.getAccountByNumber(toAccountNumber);

        BigDecimal fromBalanceBefore = fromAccount.getBalance();
        BigDecimal toBalanceBefore = toAccount.getBalance();

        // Perform the transfer
        accountService.transfer(fromAccountNumber, toAccountNumber, amount, description);

        // Refresh account data
        Account updatedFromAccount = accountService.getAccountByNumber(fromAccountNumber);
        Account updatedToAccount = accountService.getAccountByNumber(toAccountNumber);

        // Create outgoing transaction
        Transaction outgoingTransaction = Transaction.builder()
                .amount(amount)
                .transactionType(TransactionType.TRANSFER_OUT)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .account(updatedFromAccount)
                .targetAccount(updatedToAccount)
                .balanceBefore(fromBalanceBefore)
                .balanceAfter(updatedFromAccount.getBalance())
                .transactionDate(LocalDateTime.now())
                .build();

        // Create incoming transaction
        Transaction incomingTransaction = Transaction.builder()
                .amount(amount)
                .transactionType(TransactionType.TRANSFER_IN)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .account(updatedToAccount)
                .targetAccount(updatedFromAccount)
                .balanceBefore(toBalanceBefore)
                .balanceAfter(updatedToAccount.getBalance())
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction savedOutgoing = createTransaction(outgoingTransaction);
        Transaction savedIncoming = createTransaction(incomingTransaction);

        return List.of(savedOutgoing, savedIncoming);
    }

    public Optional<Transaction> findById(Long id) {
        log.debug("Finding transaction by ID: {}", id);
        return transactionRepository.findById(id);
    }

    public Optional<Transaction> findByTransactionId(String transactionId) {
        log.debug("Finding transaction by transaction ID: {}", transactionId);
        return transactionRepository.findByTransactionId(transactionId);
    }

    public Page<Transaction> findByAccountId(Long accountId, Pageable pageable) {
        log.debug("Finding transactions by account ID: {}", accountId);
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    public Page<Transaction> findByAccountNumber(String accountNumber, Pageable pageable) {
        log.debug("Finding transactions by account number: {}", accountNumber);
        return transactionRepository.findByAccountNumber(accountNumber, pageable);
    }

    public Page<Transaction> findByCustomerId(String customerId, Pageable pageable) {
        log.debug("Finding transactions by customer ID: {}", customerId);
        return transactionRepository.findByCustomerId(customerId, pageable);
    }

    public List<Transaction> findByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Finding transactions between {} and {}", startDate, endDate);
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    public List<Transaction> findByAccountAndDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Finding transactions for account {} between {} and {}", accountId, startDate, endDate);
        return transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
    }

    public List<Transaction> findByTransactionType(TransactionType transactionType) {
        log.debug("Finding transactions by type: {}", transactionType);
        return transactionRepository.findByTransactionType(transactionType);
    }

    public List<Transaction> findByStatus(TransactionStatus status) {
        log.debug("Finding transactions by status: {}", status);
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> findIncomingTransfers(Long accountId) {
        log.debug("Finding incoming transfers for account: {}", accountId);
        return transactionRepository.findIncomingTransfers(accountId);
    }

    public List<Transaction> findOutgoingTransfers(Long accountId) {
        log.debug("Finding outgoing transfers for account: {}", accountId);
        return transactionRepository.findOutgoingTransfers(accountId);
    }

    public List<Transaction> findAllTransfers(Long accountId) {
        log.debug("Finding all transfers for account: {}", accountId);
        return transactionRepository.findAllTransfers(accountId);
    }

    public BigDecimal getTotalAmountByAccountAndType(Long accountId, TransactionType transactionType) {
        log.debug("Getting total amount for account {} and type {}", accountId, transactionType);
        BigDecimal total = transactionRepository.getTotalAmountByAccountAndType(accountId, transactionType);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Long countTransactionsByAccountAndDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Counting transactions for account {} between {} and {}", accountId, startDate, endDate);
        return transactionRepository.countTransactionsByAccountAndDateRange(accountId, startDate, endDate);
    }

    public List<Object[]> getDailyTransactionSummary(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting daily transaction summary for account {} between {} and {}", accountId, startDate, endDate);
        return transactionRepository.getDailyTransactionSummary(accountId, startDate, endDate);
    }

    @Transactional
    public Transaction updateTransactionStatus(String transactionId, TransactionStatus status) {
        log.info("Updating transaction status for ID: {} to {}", transactionId, status);

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BankingException("Transaction not found with ID: " + transactionId,
                        "TRANSACTION_NOT_FOUND"));

        TransactionStatus oldStatus = transaction.getStatus();
        transaction.setStatus(status);

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Successfully updated transaction status from {} to {} for ID: {}", oldStatus, status, transactionId);
        return savedTransaction;
    }

    public List<Transaction> findPendingTransactionsOlderThan(LocalDateTime cutoffTime) {
        log.debug("Finding pending transactions older than: {}", cutoffTime);
        return transactionRepository.findPendingTransactionsOlderThan(cutoffTime);
    }

    @Transactional
    public void cleanupPendingTransactions(LocalDateTime cutoffTime) {
        log.info("Cleaning up pending transactions older than: {}", cutoffTime);

        List<Transaction> pendingTransactions = findPendingTransactionsOlderThan(cutoffTime);

        for (Transaction transaction : pendingTransactions) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
        }

        log.info("Cleaned up {} pending transactions", pendingTransactions.size());
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
    }

    public boolean existsByTransactionId(String transactionId) {
        return transactionRepository.existsByTransactionId(transactionId);
    }
}
