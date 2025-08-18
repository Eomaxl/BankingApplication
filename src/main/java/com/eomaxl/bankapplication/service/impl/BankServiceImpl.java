package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.Bank;
import com.eomaxl.bankapplication.repository.BankRepository;
import com.eomaxl.bankapplication.service.IBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BankServiceImpl implements IBankService {

    private final BankRepository bankRepository;

    @Transactional
    public Bank createBank(Bank bank) {
        log.info("Creating new bank with code: {}", bank.getBankCode());

        if (bankRepository.existsByBankCode(bank.getBankCode())) {
            throw new BankingException("Bank with code already exists: " + bank.getBankCode(), "DUPLICATE_BANK_CODE");
        }

        Bank savedBank = bankRepository.save(bank);
        log.info("Successfully created bank with ID: {}", savedBank.getId());
        return savedBank;
    }

    public Optional<Bank> findById(Long id) {
        log.debug("Finding bank by ID: {}", id);
        return bankRepository.findById(id);
    }

    public Optional<Bank> findByBankCode(String bankCode) {
        log.debug("Finding bank by code: {}", bankCode);
        return bankRepository.findByBankCode(bankCode);
    }

    public List<Bank> findByBankName(String bankName) {
        log.debug("Searching banks by name: {}", bankName);
        return bankRepository.findByBankNameContaining(bankName);
    }

    public List<Bank> findAll() {
        log.debug("Retrieving all banks");
        return bankRepository.findAll();
    }

    public Optional<Bank> findByIdWithAccounts(Long bankId) {
        log.debug("Finding bank with accounts by ID: {}", bankId);
        return bankRepository.findByIdWithAccounts(bankId);
    }

    public Long getAccountCountByBankId(Long bankId) {
        log.debug("Getting account count for bank ID: {}", bankId);
        return bankRepository.countAccountsByBankId(bankId);
    }

    @Transactional
    public Bank updateBank(Long id, Bank updatedBank) {
        log.info("Updating bank with ID: {}", id);

        Bank existingBank = bankRepository.findById(id)
                .orElseThrow(() -> new BankingException("Bank not found with ID: " + id, "BANK_NOT_FOUND"));

        // Check if bank code is being changed and if it's already taken
        if (!existingBank.getBankCode().equals(updatedBank.getBankCode()) &&
                bankRepository.existsByBankCode(updatedBank.getBankCode())) {
            throw new BankingException("Bank code already exists: " + updatedBank.getBankCode(), "DUPLICATE_BANK_CODE");
        }

        existingBank.setBankName(updatedBank.getBankName());
        existingBank.setBankCode(updatedBank.getBankCode());
        existingBank.setAddress(updatedBank.getAddress());
        existingBank.setPhoneNumber(updatedBank.getPhoneNumber());
        existingBank.setEmail(updatedBank.getEmail());

        Bank savedBank = bankRepository.save(existingBank);
        log.info("Successfully updated bank with ID: {}", savedBank.getId());
        return savedBank;
    }

    @Transactional
    public void deleteBank(Long id) {
        log.info("Deleting bank with ID: {}", id);

        Bank bank = bankRepository.findById(id)
                .orElseThrow(() -> new BankingException("Bank not found with ID: " + id, "BANK_NOT_FOUND"));

        // Check if bank has active accounts
        Long accountCount = bankRepository.countAccountsByBankId(id);
        if (accountCount > 0) {
            throw new BankingException("Cannot delete bank with active accounts. Account count: " + accountCount,
                    "BANK_HAS_ACTIVE_ACCOUNTS");
        }

        bankRepository.deleteById(id);
        log.info("Successfully deleted bank with ID: {}", id);
    }

    public boolean existsByBankCode(String bankCode) {
        return bankRepository.existsByBankCode(bankCode);
    }
}
