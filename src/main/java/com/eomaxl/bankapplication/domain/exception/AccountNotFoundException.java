package com.eomaxl.bankapplication.domain.exception;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String accountNumber) {
        super(String.format("Account not found: %s",accountNumber), "ACCOUNT_NOT_FOUND");
    }

    public AccountNotFoundException(Long accountId){
        super(String.format("Account not found with ID : %s", accountId), "ACCOUNT_NOT_FOUND");
    }
}
