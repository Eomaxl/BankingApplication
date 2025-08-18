package com.eomaxl.bankapplication.controller;

import com.eomaxl.bankapplication.dto.AccountDto;
import com.eomaxl.bankapplication.dto.TransactionDto;
import com.eomaxl.bankapplication.dto.request.CreateAccountRequest;
import com.eomaxl.bankapplication.dto.request.DepositRequest;
import com.eomaxl.bankapplication.dto.request.WithdrawalRequest;
import com.eomaxl.bankapplication.dto.response.ApiResponse;
import com.eomaxl.bankapplication.mapper.BankingMapper;
import com.eomaxl.bankapplication.service.IAccountService;
import com.eomaxl.bankapplication.service.ITransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
public class AccountController {

    private final IAccountService accountService;
    private final ITransactionService transactionService;
    private final BankingMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new account", description = "Creates a new bank account")
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("Creating account for customer: {}", request.getCustomerId());

        // This would need to be implemented in AccountService to handle the request properly
        // For now, returning a placeholder response
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account creation endpoint - implementation needed", null));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get account by number", description = "Retrieves an account by its number")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountByNumber(
            @Parameter(description = "Account number") @PathVariable String accountNumber) {
        log.info("Retrieving account: {}", accountNumber);

        return accountService.findByAccountNumber(accountNumber)
                .map(account -> {
                    var accountDto = mapper.toAccountDto(account);
                    return ResponseEntity.ok(ApiResponse.success(accountDto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountNumber}/balance")
    @Operation(summary = "Get account balance", description = "Retrieves the current balance of an account")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(
            @Parameter(description = "Account number") @PathVariable String accountNumber) {
        log.info("Retrieving balance for account: {}", accountNumber);

        var balance = accountService.getBalance(accountNumber);
        return ResponseEntity.ok(ApiResponse.success("Current balance retrieved", balance));
    }

    @PostMapping("/{accountNumber}/deposit")
    @Operation(summary = "Deposit money", description = "Deposits money into an account")
    public ResponseEntity<ApiResponse<TransactionDto>> deposit(
            @Parameter(description = "Account number") @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        log.info("Processing deposit of {} to account: {}", request.getAmount(), accountNumber);

        var transaction = transactionService.deposit(accountNumber, request.getAmount(), request.getDescription());
        var transactionDto = mapper.toTransactionDto(transaction);

        return ResponseEntity.ok(ApiResponse.success("Deposit completed successfully", transactionDto));
    }

    @PostMapping("/{accountNumber}/withdraw")
    @Operation(summary = "Withdraw money", description = "Withdraws money from an account")
    public ResponseEntity<ApiResponse<TransactionDto>> withdraw(
            @Parameter(description = "Account number") @PathVariable String accountNumber,
            @Valid @RequestBody WithdrawalRequest request) {
        log.info("Processing withdrawal of {} from account: {}", request.getAmount(), accountNumber);

        var transaction = transactionService.withdrawal(accountNumber, request.getAmount(), request.getDescription());
        var transactionDto = mapper.toTransactionDto(transaction);

        return ResponseEntity.ok(ApiResponse.success("Withdrawal completed successfully", transactionDto));
    }

    @GetMapping("/{accountNumber}/transactions")
    @Operation(summary = "Get account transactions", description = "Retrieves transactions for an account")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getAccountTransactions(
            @Parameter(description = "Account number") @PathVariable String accountNumber,
            Pageable pageable) {
        log.info("Retrieving transactions for account: {}", accountNumber);

        var transactions = transactionService.findByAccountNumber(accountNumber, pageable);
        var transactionDtos = transactions.map(mapper::toTransactionDto);

        return ResponseEntity.ok(ApiResponse.success(transactionDtos));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get accounts by customer ID", description = "Retrieves all accounts for a customer")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccountsByCustomerId(
            @Parameter(description = "Customer ID") @PathVariable String customerId) {
        log.info("Retrieving accounts for customer: {}", customerId);

        var accounts = accountService.findByCustomerId(customerId);
        var accountDtos = mapper.toAccountDtos(accounts);

        return ResponseEntity.ok(ApiResponse.success(accountDtos));
    }

    @GetMapping("/bank/{bankId}")
    @Operation(summary = "Get accounts by bank ID", description = "Retrieves all accounts for a bank")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAccountsByBankId(
            @Parameter(description = "Bank ID") @PathVariable Long bankId) {
        log.info("Retrieving accounts for bank: {}", bankId);

        var accounts = accountService.findByBankId(bankId);
        var accountDtos = mapper.toAccountDtos(accounts);

        return ResponseEntity.ok(ApiResponse.success(accountDtos));
    }

    @GetMapping("/high-value")
    @Operation(summary = "Get high value accounts", description = "Retrieves accounts with balance above threshold")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getHighValueAccounts(
            @Parameter(description = "Minimum balance threshold") @RequestParam BigDecimal threshold) {
        log.info("Retrieving high value accounts with threshold: {}", threshold);

        var accounts = accountService.findHighValueAccounts(threshold);
        var accountDtos = mapper.toAccountDtos(accounts);

        return ResponseEntity.ok(ApiResponse.success(accountDtos));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update account status", description = "Updates the status of an account")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccountStatus(
            @Parameter(description = "Account ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam String status) {
        log.info("Updating account status for ID: {} to {}", id, status);

        var accountStatus = com.eomaxl.bankapplication.domain.model.AccountStatus.valueOf(status.toUpperCase());
        var updatedAccount = accountService.updateAccountStatus(id, accountStatus);
        var accountDto = mapper.toAccountDto(updatedAccount);

        return ResponseEntity.ok(ApiResponse.success("Account status updated successfully", accountDto));
    }
}
