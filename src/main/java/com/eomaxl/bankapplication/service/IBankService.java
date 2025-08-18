package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Bank;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Bank management operations
 * Provides contract for bank-related business logic
 */
public interface IBankService {

    /**
     * Creates a new bank in the system
     * @param bank Bank entity to create
     * @return Created bank with generated ID
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if bank code already exists
     */
    Bank createBank(Bank bank);

    /**
     * Finds a bank by its unique ID
     * @param id Bank ID
     * @return Optional containing bank if found
     */
    Optional<Bank> findById(Long id);

    /**
     * Finds a bank by its unique bank code
     * @param bankCode Bank code (e.g., "FNB001")
     * @return Optional containing bank if found
     */
    Optional<Bank> findByBankCode(String bankCode);

    /**
     * Searches for banks by name
     * @param bankName Bank name to search for (case-insensitive partial match)
     * @return List of matching banks
     */
    List<Bank> findByBankName(String bankName);

    /**
     * Retrieves all banks in the system
     * @return List of all banks
     */
    List<Bank> findAll();

    /**
     * Finds a bank with its associated accounts loaded
     * @param bankId Bank ID
     * @return Optional containing bank with accounts if found
     */
    Optional<Bank> findByIdWithAccounts(Long bankId);

    /**
     * Gets the count of accounts for a specific bank
     * @param bankId Bank ID
     * @return Number of accounts associated with the bank
     */
    Long getAccountCountByBankId(Long bankId);

    /**
     * Updates an existing bank's information
     * @param id Bank ID to update
     * @param updatedBank Updated bank data
     * @return Updated bank entity
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if bank not found or code conflict
     */
    Bank updateBank(Long id, Bank updatedBank);

    /**
     * Deletes a bank from the system
     * @param id Bank ID to delete
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if bank not found or has active accounts
     */
    void deleteBank(Long id);

    /**
     * Checks if a bank exists with the given bank code
     * @param bankCode Bank code to check
     * @return true if bank exists with this code
     */
    boolean existsByBankCode(String bankCode);
}
