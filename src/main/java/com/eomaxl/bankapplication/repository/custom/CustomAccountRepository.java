package com.eomaxl.bankapplication.repository.custom;

import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.AccountStatus;
import com.eomaxl.bankapplication.domain.model.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomAccountRepository {
    List<Account> findAccountsWithComplexCriteria(String customerName,
                                                  AccountType accountType,
                                                  AccountStatus status,
                                                  BigDecimal minBalance,
                                                  BigDecimal maxBalance);

    Page<Account> searchAccounts(String searchTerm, Pageable pageable);

    List<Account> findDormantAccounts(LocalDateTime lastTransactionDate);

    List<Account> findHighValueAccounts(BigDecimal threshold);

    List<Object[]> getAccountSummaryByBank();
}
