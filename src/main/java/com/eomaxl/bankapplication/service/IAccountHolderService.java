package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.AccountHolder;
import com.eomaxl.bankapplication.domain.model.Person;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for AccountHolder management operations
 * Provides contract for account holder-related business logic
 */
public interface IAccountHolderService {

    /**
     * Creates a new account holder for a person
     * @param person Person to create account holder for
     * @return Created account holder with generated customer ID
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account holder already exists for person
     */
    AccountHolder createAccountHolder(Person person);

    /**
     * Finds an account holder by their unique ID
     * @param id Account holder ID
     * @return Optional containing account holder if found
     */
    Optional<AccountHolder> findById(Long id);

    /**
     * Finds an account holder by their customer ID
     * @param customerId Customer ID (e.g., "CUST001")
     * @return Optional containing account holder if found
     */
    Optional<AccountHolder> findByCustomerId(String customerId);

    /**
     * Finds an account holder by their person's email
     * @param email Person's email address
     * @return Optional containing account holder if found
     */
    Optional<AccountHolder> findByPersonEmail(String email);

    /**
     * Finds account holders by their status
     * @param status Account holder status
     * @return List of account holders with the specified status
     */
    List<AccountHolder> findByStatus(AccountHolder.AccountHolderStatus status);

    /**
     * Finds an account holder with their associated accounts loaded
     * @param accountHolderId Account holder ID
     * @return Optional containing account holder with accounts if found
     */
    Optional<AccountHolder> findByIdWithAccounts(Long accountHolderId);

    /**
     * Gets the count of accounts for a specific account holder
     * @param accountHolderId Account holder ID
     * @return Number of accounts associated with the account holder
     */
    Long getAccountCountByAccountHolderId(Long accountHolderId);

    /**
     * Updates an account holder's status
     * @param id Account holder ID
     * @param status New status
     * @return Updated account holder
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account holder not found
     */
    AccountHolder updateAccountHolderStatus(Long id, AccountHolder.AccountHolderStatus status);

    /**
     * Updates an account holder's information
     * @param id Account holder ID
     * @param updatedAccountHolder Updated account holder data
     * @return Updated account holder
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account holder not found
     */
    AccountHolder updateAccountHolder(Long id, AccountHolder updatedAccountHolder);

    /**
     * Deletes an account holder from the system
     * @param id Account holder ID to delete
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account holder not found or has active accounts
     */
    void deleteAccountHolder(Long id);

    /**
     * Retrieves all account holders in the system
     * @return List of all account holders
     */
    List<AccountHolder> findAll();

    /**
     * Checks if an account holder exists with the given customer ID
     * @param customerId Customer ID to check
     * @return true if account holder exists with this customer ID
     */
    boolean existsByCustomerId(String customerId);
}
