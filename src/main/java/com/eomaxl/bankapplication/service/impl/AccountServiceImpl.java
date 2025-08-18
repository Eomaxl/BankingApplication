package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.AccountNotFoundException;
import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.exception.InsufficientFundsException;
import com.eomaxl.bankapplication.domain.model.*;
import com.eomaxl.bankapplication.repository.AccountRepository;
import com.eomaxl.bankapplication.service.IAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
public class AccountServiceImpl implements IAccountService {

    private final AccountRepository accountRepository;
    private final BankService bankService;
    private final AccountHolderService accountHolderService;

    @Transactional
    public Account createAccount(Account account) {
        log.info("Creating new account for account holder: {}", account.getAccountHolder().getCustomerId());

        // Validate bank exists
        Bank bank = bankService.findById(account.getBank().getId())
                .orElseThrow(() -> new BankingException("Bank not found with ID: " + account.getBank().getId(), "BANK_NOT_FOUND"));

        // Validate account holder exists
        AccountHolder accountHolder = accountHolderService.findById(account.getAccountHolder().getId())
                .orElseThrow(() -> new BankingException("Account holder not found with ID: " + account.getAccountHolder().getId(),
                        "ACCOUNT_HOLDER_NOT_FOUND"));

        // Generate unique account number
        String accountNumber = generateAccountNumber(bank.getBankCode());
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = generateAccountNumber(bank.getBankCode());
        }

        account.setAccountNumber(accountNumber);
        account.setBank(bank);
        account.setAccountHolder(accountHolder);

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }

        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully created account with number: {}", savedAccount.getAccountNumber());
        return savedAccount;
    }

    public Optional<Account> findById(Long id) {
        log.debug("Finding account by ID: {}", id);
        return accountRepository.findById(id);
    }

    @Cacheable(value = "accounts", key = "#accountNumber")
    public Optional<Account> findByAccountNumber(String accountNumber) {
        log.debug("Finding account by number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public Account getAccountByNumber(String accountNumber) {
        return findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    public List<Account> findByAccountHolderId(Long accountHolderId) {
        log.debug("Finding accounts by account holder ID: {}", accountHolderId);
        return accountRepository.findByAccountHolderId(accountHolderId);
    }

    public List<Account> findByCustomerId(String customerId) {
        log.debug("Finding accounts by customer ID: {}", customerId);
        return accountRepository.findByCustomerId(customerId);
    }

    public List<Account> findByBankId(Long bankId) {
        log.debug("Finding accounts by bank ID: {}", bankId);
        return accountRepository.findByBankId(bankId);
    }

    @Cacheable(value = "balances", key = "#accountNumber")
    public BigDecimal getBalance(String accountNumber) {
        log.debug("Getting balance for account: {}", accountNumber);
        Account account = getAccountByNumber(accountNumber);
        return account.getBalance();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, timeout = 30)
    @CacheEvict(value = {"accounts", "balances"}, key = "#accountNumber")
    public Account credit(String accountNumber, BigDecimal amount, String description) {
        log.info("Crediting amount {} to account: {}", amount, accountNumber);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Credit amount must be positive", "INVALID_AMOUNT");
        }

        // Pessimistic locking ensures consistency under high concurrency
        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Account is not active: " + accountNumber, "ACCOUNT_NOT_ACTIVE");
        }

        BigDecimal oldBalance = account.getBalance();
        account.credit(amount);

        // Force immediate flush to ensure consistency
        Account savedAccount = accountRepository.saveAndFlush(account);
        log.info("Successfully credited {} to account {}. Balance: {} -> {}",
                amount, accountNumber, oldBalance, savedAccount.getBalance());

        return savedAccount;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Account debit(String accountNumber, BigDecimal amount, String description) {
        log.info("Debiting amount {} from account: {}", amount, accountNumber);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Debit amount must be positive", "INVALID_AMOUNT");
        }

        Account account = accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Account is not active: " + accountNumber, "ACCOUNT_NOT_ACTIVE");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, amount, account.getBalance());
        }

        BigDecimal oldBalance = account.getBalance();
        account.debit(amount);

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully debited {} from account {}. Balance: {} -> {}",
                amount, accountNumber, oldBalance, savedAccount.getBalance());

        return savedAccount;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String description) {
        log.info("Transferring {} from {} to {}", amount, fromAccountNumber, toAccountNumber);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be positive", "INVALID_AMOUNT");
        }

        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new BankingException("Cannot transfer to the same account", "SAME_ACCOUNT_TRANSFER");
        }

        // Lock both accounts in a consistent order to prevent deadlocks
        String firstLock = fromAccountNumber.compareTo(toAccountNumber) < 0 ? fromAccountNumber : toAccountNumber;
        String secondLock = fromAccountNumber.compareTo(toAccountNumber) < 0 ? toAccountNumber : fromAccountNumber;

        Account firstAccount = accountRepository.findByAccountNumberWithLock(firstLock)
                .orElseThrow(() -> new AccountNotFoundException(firstLock));
        Account secondAccount = accountRepository.findByAccountNumberWithLock(secondLock)
                .orElseThrow(() -> new AccountNotFoundException(secondLock));

        Account fromAccount = fromAccountNumber.equals(firstLock) ? firstAccount : secondAccount;
        Account toAccount = toAccountNumber.equals(firstLock) ? firstAccount : secondAccount;

        // Validate accounts are active
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Source account is not active: " + fromAccountNumber, "ACCOUNT_NOT_ACTIVE");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BankingException("Destination account is not active: " + toAccountNumber, "ACCOUNT_NOT_ACTIVE");
        }

        // Check sufficient funds
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromAccountNumber, amount, fromAccount.getBalance());
        }

        // Perform transfer
        BigDecimal fromOldBalance = fromAccount.getBalance();
        BigDecimal toOldBalance = toAccount.getBalance();

        fromAccount.debit(amount);
        toAccount.credit(amount);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        log.info("Successfully transferred {} from {} (Balance: {} -> {}) to {} (Balance: {} -> {})",
                amount, fromAccountNumber, fromOldBalance, fromAccount.getBalance(),
                toAccountNumber, toOldBalance, toAccount.getBalance());
    }

    @Transactional
    public Account updateAccountStatus(Long id, AccountStatus status) {
        log.info("Updating account status for ID: {} to {}", id, status);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BankingException("Account not found with ID: " + id, "ACCOUNT_NOT_FOUND"));

        AccountStatus oldStatus = account.getStatus();
        account.setStatus(status);

        Account savedAccount = accountRepository.save(account);
        log.info("Successfully updated account status from {} to {} for ID: {}", oldStatus, status, id);
        return savedAccount;
    }

    public List<Account> findByBalanceRange(BigDecimal minBalance, BigDecimal maxBalance) {
        log.debug("Finding accounts with balance between {} and {}", minBalance, maxBalance);
        return accountRepository.findByBalanceBetween(minBalance, maxBalance);
    }

    public List<Account> findByAccountType(AccountType accountType) {
        log.debug("Finding accounts by type: {}", accountType);
        return accountRepository.findByAccountType(accountType);
    }

    public List<Account> findByStatus(AccountStatus status) {
        log.debug("Finding accounts by status: {}", status);
        return accountRepository.findByStatus(status);
    }

    public Page<Account> findActiveAccountsByAccountHolder(Long accountHolderId, Pageable pageable) {
        log.debug("Finding active accounts for account holder: {}", accountHolderId);
        return accountRepository.findActiveAccountsByAccountHolder(accountHolderId, pageable);
    }

    public BigDecimal getTotalBalanceByBankId(Long bankId) {
        log.debug("Getting total balance for bank ID: {}", bankId);
        return accountRepository.getTotalBalanceByBankId(bankId);
    }

    public List<Account> findDormantAccounts(LocalDateTime lastTransactionDate) {
        log.debug("Finding dormant accounts with last transaction before: {}", lastTransactionDate);
        return accountRepository.findDormantAccounts(lastTransactionDate);
    }

    public List<Account> findHighValueAccounts(BigDecimal threshold) {
        log.debug("Finding high value accounts with balance >= {}", threshold);
        return accountRepository.findHighValueAccounts(threshold);
    }

    private String generateAccountNumber(String bankCode) {
        return bankCode + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10).toUpperCase();
    }

    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }
}
