package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.AccountHolder;
import com.eomaxl.bankapplication.domain.model.Person;

import java.util.List;
import java.util.Optional;

public interface AccountHolderService {

    AccountHolder createAccountHolder(Person person);

    Optional<AccountHolder> findById(Long id);

    Optional<AccountHolder> findByCustomerId(String customerId);

    Optional<AccountHolder> findByPersonEmail(String email);

    List<AccountHolder> findByStatus(AccountHolder.AccountHolderStatus status);

    Optional<AccountHolder> findByIdWithAccounts(Long accountHolderId);

    Long getAccountCountByAccountHolderId(Long accountHolderId);

    AccountHolder updateAccountHolderStatus(Long id, AccountHolder.AccountHolderStatus status);

    AccountHolder updateAccountHolder(Long id, AccountHolder accountHolder);

    void deleteAccountHolder(Long id);

    List<AccountHolder> findAll();

    boolean existsByCustomerId(String customerId);
}
