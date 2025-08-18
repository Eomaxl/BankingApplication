package com.eomaxl.bankapplication.controller;

import com.eomaxl.bankapplication.dto.AccountDto;
import com.eomaxl.bankapplication.dto.request.CustomerOnboardingRequest;
import com.eomaxl.bankapplication.dto.response.ApiResponse;
import com.eomaxl.bankapplication.mapper.BankingMapper;
import com.eomaxl.bankapplication.service.impl.BankingFacadeServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/banking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Banking Operations", description = "High-level banking operations and customer management")
public class BankingController {

    private final BankingFacadeServiceImpl bankingFacadeService;
    private final BankingMapper mapper;

    @PostMapping("/onboard-customer")
    @Operation(summary = "Onboard new customer", description = "Complete customer onboarding with account creation")
    public ResponseEntity<ApiResponse<AccountDto>> onboardCustomer(@Valid @RequestBody CustomerOnboardingRequest request) {
        log.info("Onboarding new customer: {}", request.getPerson().getEmail());

        var person = mapper.toPerson(request.getPerson());
        var account = bankingFacadeService.onboardCustomer(
                person,
                request.getBankCode(),
                request.getAccountType(),
                request.getInitialDeposit()
        );

        var accountDto = mapper.toAccountDto(account);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer onboarded successfully", accountDto));
    }

    @GetMapping("/customer-profile/{customerId}")
    @Operation(summary = "Get customer profile", description = "Retrieves complete customer profile with all accounts")
    public ResponseEntity<ApiResponse<BankingFacadeServiceImpl.CustomerProfile>> getCustomerProfile(
            @Parameter(description = "Customer ID") @PathVariable String customerId) {
        log.info("Retrieving customer profile for: {}", customerId);

        var customerProfile = bankingFacadeService.getCustomerProfile(customerId);

        return ResponseEntity.ok(ApiResponse.success(customerProfile));
    }

    @GetMapping("/bank-summary/{bankCode}")
    @Operation(summary = "Get bank summary", description = "Retrieves bank summary with statistics")
    public ResponseEntity<ApiResponse<BankingFacadeServiceImpl.BankSummary>> getBankSummary(
            @Parameter(description = "Bank code") @PathVariable String bankCode) {
        log.info("Retrieving bank summary for: {}", bankCode);

        var bankSummary = bankingFacadeService.getBankSummary(bankCode);

        return ResponseEntity.ok(ApiResponse.success(bankSummary));
    }

    @PostMapping("/close-account/{accountNumber}")
    @Operation(summary = "Close account", description = "Closes an account with proper validations")
    public ResponseEntity<ApiResponse<Void>> closeAccount(
            @Parameter(description = "Account number") @PathVariable String accountNumber,
            @Parameter(description = "Reason for closure") @RequestParam String reason) {
        log.info("Closing account: {} with reason: {}", accountNumber, reason);

        bankingFacadeService.closeAccount(accountNumber, reason);

        return ResponseEntity.ok(ApiResponse.success("Account closed successfully", null));
    }
}
