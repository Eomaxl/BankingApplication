package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.AccountType;
import com.eomaxl.bankapplication.domain.model.Person;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Facade service interface for high-level banking operations
 * Provides contract for complex business workflows that orchestrate multiple services
 */
public interface IBankingFacadeService {
    /**
     * Complete customer onboarding process
     * Creates person, account holder, and account in a single transaction
     * @param person Person information
     * @param bankCode Bank code where account will be created
     * @param accountType Type of account to create
     * @param initialDeposit Initial deposit amount (can be null or zero)
     * @return Created account with all relationships established
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if onboarding fails
     */
    Account onboardCustomer(Person person, String bankCode, AccountType accountType, BigDecimal initialDeposit);

    /**
     * Retrieves complete customer profile with all accounts and total balance
     * @param customerId Customer ID
     * @return Customer profile with accounts and total balance
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if customer not found
     */
    BankingFacadeService.CustomerProfile getCustomerProfile(String customerId);

    /**
     * Performs money transfer with complete transaction logging and error handling
     * @param fromAccountNumber Source account number
     * @param toAccountNumber Destination account number
     * @param amount Amount to transfer
     * @param description Transfer description
     * @return Transfer result with success status and transaction details
     */
    BankingFacadeService.TransferResult performTransfer(String fromAccountNumber, String toAccountNumber,
                                                        BigDecimal amount, String description);

    /**
     * Generates account statement with transactions for a date range
     * @param accountNumber Account number
     * @param startDate Statement start date
     * @param endDate Statement end date
     * @param pageable Pagination for transactions
     * @return Account statement with transaction summary
     * @throws com.eomaxl.bankapplication.domain.exception.AccountNotFoundException if account not found
     */
    BankingFacadeService.AccountStatement getAccountStatement(String accountNumber, LocalDateTime startDate,
                                                              LocalDateTime endDate, Pageable pageable);

    /**
     * Closes an account with proper validations
     * Ensures zero balance and creates closure transaction record
     * @param accountNumber Account number to close
     * @param reason Reason for account closure
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if account has non-zero balance or other issues
     */
    void closeAccount(String accountNumber, String reason);

    /**
     * Generates comprehensive bank summary with statistics
     * @param bankCode Bank code
     * @return Bank summary with account counts, total balance, and averages
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if bank not found
     */
    BankingFacadeService.BankSummary getBankSummary(String bankCode);
}
