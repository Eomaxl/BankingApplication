package com.eomaxl.bankapplication.repository;

import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.AccountStatus;
import com.eomaxl.bankapplication.domain.model.AccountType;
import com.eomaxl.bankapplication.repository.custom.CustomAccountRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long>, CustomAccountRepository {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.accountHolder.id = :accountHolder")
    List<Account> findByAccountHolderId(@Param("accountHolderId") Long accountHolderId);

    @Query("SELECT a FROM Account a WHERE a.bank.id = :bankId")
    List<Account> findByBankId(@Param("bankId") Long bankId);

    @Query("SELECT a FROM Account a WHERE a.accountType = :accountType")
    List<Account> findByAccountType(@Param("accountType") AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.status = :status")
    List<Account> findByStatus(@Param("status") AccountStatus status);

    @Query("SELECT a FROM Account a WHERE a.balance >= :minBalance AND a.balance <= :maxBalance")
    List<Account> findByBalanceBetween(@Param("minBalance") BigDecimal minBalance, @Param("maxBalance") BigDecimal maxBalance);

    @Query("SELECT a FROM Account a WHERE a.accountHolder.customerId = :customerId")
    List<Account> findByCustomerId(@Param("customerId") String customerId);

    @Query("SELECT a FROM Account a JOIN FETCH a.transactions WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithTransactions(@Param("accountNumber") String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<Account> findAccountsCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT a FROM Account a WHERE a.accountHolder.person.email = :email")
    List<Account> findByAccountHolderEmail(@Param("email") String email);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.bank.id = :bankId")
    BigDecimal getTotalBalanceByBankId(@Param("bankId") Long bankId);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountType = :accountType AND a.status = 'ACTIVE'")
    Long countActiveAccountsByType(@Param("accountType") AccountType accountType);

    @Modifying
    @Query("UPDATE Account a SET a.status = :status WHERE a.id = :accountId")
    int updateAccountStatus(@Param("accountId") Long accountId, @Param("status") AccountStatus status);

    @Query("SELECT a FROM Account a WHERE a.accountHolder.id = :accountHolderId AND a.status = 'ACTIVE'")
    Page<Account> findActiveAccountsByAccountHolder(@Param("accountHolderId") Long accountHolderId, Pageable pageable);
}
