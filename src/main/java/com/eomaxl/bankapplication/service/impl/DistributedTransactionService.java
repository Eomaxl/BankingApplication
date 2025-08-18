package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.service.IAccountService;
import com.eomaxl.bankapplication.service.ITransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedTransactionService {

    private final IAccountService accountService;
    private final ITransactionService transactionService;

    // Account-level locks to prevent deadlocks while maintaining consistency
    private final ConcurrentHashMap<String, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    /**
     * Atomic money transfer with strong consistency guarantees
     * Uses ordered locking to prevent deadlocks
     * SERIALIZABLE isolation ensures no phantom reads or dirty reads
     */
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            propagation = Propagation.REQUIRED,
            timeout = 30,
            rollbackFor = Exception.class
    )
    public TransferResult atomicTransfer(String fromAccount, String toAccount,
                                         BigDecimal amount, String description) {

        log.info("Starting atomic transfer: {} from {} to {}", amount, fromAccount, toAccount);

        if (fromAccount.equals(toAccount)) {
            throw new BankingException("Cannot transfer to the same account", "SAME_ACCOUNT_TRANSFER");
        }

        // Ordered locking to prevent deadlocks
        String firstLock = fromAccount.compareTo(toAccount) < 0 ? fromAccount : toAccount;
        String secondLock = fromAccount.compareTo(toAccount) < 0 ? toAccount : fromAccount;

        ReentrantLock lock1 = accountLocks.computeIfAbsent(firstLock, k -> new ReentrantLock());
        ReentrantLock lock2 = accountLocks.computeIfAbsent(secondLock, k -> new ReentrantLock());

        lock1.lock();
        try {
            lock2.lock();
            try {
                return performAtomicTransfer(fromAccount, toAccount, amount, description);
            } finally {
                lock2.unlock();
            }
        } finally {
            lock1.unlock();
        }
    }

    private TransferResult performAtomicTransfer(String fromAccount, String toAccount,
                                                 BigDecimal amount, String description) {

        LocalDateTime startTime = LocalDateTime.now();

        try {
            // Step 1: Validate and lock both accounts
            Account sourceAccount = accountService.getAccountByNumber(fromAccount);
            Account targetAccount = accountService.getAccountByNumber(toAccount);

            // Step 2: Pre-flight checks
            validateTransferPreconditions(sourceAccount, targetAccount, amount);

            // Step 3: Record balances before transaction
            BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
            BigDecimal targetBalanceBefore = targetAccount.getBalance();

            // Step 4: Perform atomic debit and credit
            accountService.debit(fromAccount, amount, description);
            accountService.credit(toAccount, amount, description);

            // Step 5: Create transaction records
            Transaction debitTxn = createTransactionRecord(sourceAccount, amount,
                    TransactionType.TRANSFER_OUT, description, targetAccount);
            Transaction creditTxn = createTransactionRecord(targetAccount, amount,
                    TransactionType.TRANSFER_IN, description, sourceAccount);

            // Step 6: Save transaction records
            transactionService.createTransaction(debitTxn);
            transactionService.createTransaction(creditTxn);

            // Step 7: Get final balances
            BigDecimal sourceBalanceAfter = accountService.getBalance(fromAccount);
            BigDecimal targetBalanceAfter = accountService.getBalance(toAccount);

            TransferResult result = TransferResult.builder()
                    .success(true)
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(amount)
                    .sourceBalanceBefore(sourceBalanceBefore)
                    .sourceBalanceAfter(sourceBalanceAfter)
                    .targetBalanceBefore(targetBalanceBefore)
                    .targetBalanceAfter(targetBalanceAfter)
                    .transactionTime(startTime)
                    .processingTimeMs(java.time.Duration.between(startTime, LocalDateTime.now()).toMillis())
                    .build();

            log.info("Atomic transfer completed successfully: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Atomic transfer failed: {} from {} to {}", amount, fromAccount, toAccount, e);

            return TransferResult.builder()
                    .success(false)
                    .fromAccount(fromAccount)
                    .toAccount(toAccount)
                    .amount(amount)
                    .errorMessage(e.getMessage())
                    .transactionTime(startTime)
                    .processingTimeMs(java.time.Duration.between(startTime, LocalDateTime.now()).toMillis())
                    .build();
        }
    }

    private void validateTransferPreconditions(Account sourceAccount, Account targetAccount, BigDecimal amount) {
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Source account is not active", "ACCOUNT_NOT_ACTIVE");
        }

        if (targetAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Target account is not active", "ACCOUNT_NOT_ACTIVE");
        }

        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new BankingException("Insufficient funds", "INSUFFICIENT_FUNDS");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be positive", "INVALID_AMOUNT");
        }
    }

    private Transaction createTransactionRecord(Account account, BigDecimal amount,
                                                TransactionType type, String description,
                                                Account targetAccount) {
        return Transaction.builder()
                .amount(amount)
                .transactionType(type)
                .status(TransactionStatus.COMPLETED)
                .description(description)
                .account(account)
                .targetAccount(targetAccount)
                .balanceBefore(account.getBalance())
                .balanceAfter(type == TransactionType.TRANSFER_OUT ?
                        account.getBalance().subtract(amount) :
                        account.getBalance().add(amount))
                .transactionDate(LocalDateTime.now())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class TransferResult {
        private boolean success;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private BigDecimal sourceBalanceBefore;
        private BigDecimal sourceBalanceAfter;
        private BigDecimal targetBalanceBefore;
        private BigDecimal targetBalanceAfter;
        private LocalDateTime transactionTime;
        private long processingTimeMs;
        private String errorMessage;
    }
}
