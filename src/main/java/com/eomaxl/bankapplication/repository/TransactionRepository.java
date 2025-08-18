package com.eomaxl.bankapplication.repository;

import com.eomaxl.bankapplication.domain.model.Transaction;
import com.eomaxl.bankapplication.domain.model.TransactionStatus;
import com.eomaxl.bankapplication.domain.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    @Query("SELECT t FROM Transaction  t WHERE t.account.id = :accountId ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountId(@Param("accountId") Long accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.account.accountNumber = :accountNumber ORDER BY t.transactionDate DESC")
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);

    @Query("SELECT t FROM Transaction  t WHERE t.transactionType = :transactionType")
    List<Transaction> findByTransactionType(@Param("transactionType") TransactionType transactionType);

    @Query("SELECT t FROM Transaction t WHERE t.status = :status")
    List<Transaction> findByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT t FROM Transaction t WHERE t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByTransactionDateBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdAndDateRange(@Param("accountId") Long accountId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.amount >= :minAmount AND t.amount <= :maxAmount")
    List<Transaction> findByAmountBetween(@Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount);

    @Query("SELECT t FROM Transaction t WHERE t.account.accountHolder.customerId = :customerId " +
            "ORDER BY t.transactionDate DESC")
    Page<Transaction> findByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionType = :transactionType AND t.status = 'COMPLETED'")
    BigDecimal getTotalAmountByAccountAndType(@Param("accountId") Long accountId,
                                              @Param("transactionType") TransactionType transactionType);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Long countTransactionsByAccountAndDateRange(@Param("accountId") Long accountId,
                                                @Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.targetAccount.id = :accountId " +
            "AND t.transactionType = 'TRANSFER_IN' ORDER BY t.transactionDate DESC")
    List<Transaction> findIncomingTransfers(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionType = 'TRANSFER_OUT' ORDER BY t.transactionDate DESC")
    List<Transaction> findOutgoingTransfers(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE (t.account.id = :accountId OR t.targetAccount.id = :accountId) " +
            "AND t.transactionType IN ('TRANSFER_IN', 'TRANSFER_OUT') " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findAllTransfers(@Param("accountId") Long accountId);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' " +
            "AND t.transactionDate < :cutoffTime")
    List<Transaction> findPendingTransactionsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT DATE(t.transactionDate) as date, COUNT(t) as count, SUM(t.amount) as total " +
            "FROM Transaction t WHERE t.account.id = :accountId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(t.transactionDate) ORDER BY DATE(t.transactionDate)")
    List<Object[]> getDailyTransactionSummary(@Param("accountId") Long accountId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);
}
