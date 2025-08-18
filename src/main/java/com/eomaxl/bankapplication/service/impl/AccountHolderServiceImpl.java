package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.AccountHolder;
import com.eomaxl.bankapplication.domain.model.Person;
import com.eomaxl.bankapplication.repository.AccountHolderRepository;
import com.eomaxl.bankapplication.service.IAccountHolderService;
import com.eomaxl.bankapplication.service.IPersonService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AccountHolderServiceImpl implements IAccountHolderService {
    private final AccountHolderRepository accountHolderRepository;
    private final IPersonService personService;

    @Transactional
    public AccountHolder createAccountHolder(Person person) {
        log.info("Creating account holder for person: {}", person.getEmail());

        // Check if person already has an account holder record
        Optional<AccountHolder> existingAccountHolder = accountHolderRepository.findByPersonEmail(person.getEmail());
        if (existingAccountHolder.isPresent()) {
            throw new BankingException("Account holder already exists for person: " + person.getEmail(),
                    "ACCOUNT_HOLDER_EXISTS");
        }

        // Create or get existing person
        Person savedPerson;
        if (person.getId() == null) {
            savedPerson = personService.createPerson(person);
        } else {
            savedPerson = personService.findById(person.getId())
                    .orElseThrow(() -> new BankingException("Person not found with ID: " + person.getId(), "PERSON_NOT_FOUND"));
        }

        // Generate unique customer ID
        String customerId = generateCustomerId();
        while (accountHolderRepository.existsByCustomerId(customerId)) {
            customerId = generateCustomerId();
        }

        AccountHolder accountHolder = AccountHolder.builder()
                .person(savedPerson)
                .customerId(customerId)
                .status(AccountHolder.AccountHolderStatus.ACTIVE)
                .build();

        AccountHolder savedAccountHolder = accountHolderRepository.save(accountHolder);
        log.info("Successfully created account holder with customer ID: {}", savedAccountHolder.getCustomerId());
        return savedAccountHolder;
    }

    public Optional<AccountHolder> findById(Long id) {
        log.debug("Finding account holder by ID: {}", id);
        return accountHolderRepository.findById(id);
    }

    public Optional<AccountHolder> findByCustomerId(String customerId) {
        log.debug("Finding account holder by customer ID: {}", customerId);
        return accountHolderRepository.findByCustomerId(customerId);
    }

    public Optional<AccountHolder> findByPersonEmail(String email) {
        log.debug("Finding account holder by person email: {}", email);
        return accountHolderRepository.findByPersonEmail(email);
    }

    public List<AccountHolder> findByStatus(AccountHolder.AccountHolderStatus status) {
        log.debug("Finding account holders by status: {}", status);
        return accountHolderRepository.findByStatus(status);
    }

    public Optional<AccountHolder> findByIdWithAccounts(Long accountHolderId) {
        log.debug("Finding account holder with accounts by ID: {}", accountHolderId);
        return accountHolderRepository.findByIdWithAccounts(accountHolderId);
    }

    public Long getAccountCountByAccountHolderId(Long accountHolderId) {
        log.debug("Getting account count for account holder ID: {}", accountHolderId);
        return accountHolderRepository.countAccountsByAccountHolderId(accountHolderId);
    }

    @Transactional
    public AccountHolder updateAccountHolderStatus(Long id, AccountHolder.AccountHolderStatus status) {
        log.info("Updating account holder status for ID: {} to {}", id, status);

        AccountHolder accountHolder = accountHolderRepository.findById(id)
                .orElseThrow(() -> new BankingException("Account holder not found with ID: " + id, "ACCOUNT_HOLDER_NOT_FOUND"));

        AccountHolder.AccountHolderStatus oldStatus = accountHolder.getStatus();
        accountHolder.setStatus(status);

        AccountHolder savedAccountHolder = accountHolderRepository.save(accountHolder);
        log.info("Successfully updated account holder status from {} to {} for ID: {}", oldStatus, status, id);
        return savedAccountHolder;
    }

    @Transactional
    public AccountHolder updateAccountHolder(Long id, AccountHolder updatedAccountHolder) {
        log.info("Updating account holder with ID: {}", id);

        AccountHolder existingAccountHolder = accountHolderRepository.findById(id)
                .orElseThrow(() -> new BankingException("Account holder not found with ID: " + id, "ACCOUNT_HOLDER_NOT_FOUND"));

        // Update person information if provided
        if (updatedAccountHolder.getPerson() != null) {
            Person updatedPerson = personService.updatePerson(existingAccountHolder.getPerson().getId(),
                    updatedAccountHolder.getPerson());
            existingAccountHolder.setPerson(updatedPerson);
        }

        // Update status if provided
        if (updatedAccountHolder.getStatus() != null) {
            existingAccountHolder.setStatus(updatedAccountHolder.getStatus());
        }

        AccountHolder savedAccountHolder = accountHolderRepository.save(existingAccountHolder);
        log.info("Successfully updated account holder with ID: {}", savedAccountHolder.getId());
        return savedAccountHolder;
    }

    @Transactional
    public void deleteAccountHolder(Long id) {
        log.info("Deleting account holder with ID: {}", id);

        AccountHolder accountHolder = accountHolderRepository.findById(id)
                .orElseThrow(() -> new BankingException("Account holder not found with ID: " + id, "ACCOUNT_HOLDER_NOT_FOUND"));

        // Check if account holder has active accounts
        Long accountCount = accountHolderRepository.countAccountsByAccountHolderId(id);
        if (accountCount > 0) {
            throw new BankingException("Cannot delete account holder with active accounts. Account count: " + accountCount,
                    "ACCOUNT_HOLDER_HAS_ACTIVE_ACCOUNTS");
        }

        accountHolderRepository.deleteById(id);
        log.info("Successfully deleted account holder with ID: {}", id);
    }

    public List<AccountHolder> findAll() {
        log.debug("Retrieving all account holders");
        return accountHolderRepository.findAll();
    }

    private String generateCustomerId() {
        return "CUST" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public boolean existsByCustomerId(String customerId) {
        return accountHolderRepository.existsByCustomerId(customerId);
    }

}
