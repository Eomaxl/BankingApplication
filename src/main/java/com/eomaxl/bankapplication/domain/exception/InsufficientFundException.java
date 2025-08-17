package com.eomaxl.bankapplication.domain.exception;

import java.math.BigDecimal;

public class InsufficientFundException extends BankingException {
    public InsufficientFundException(String accountNumber, BigDecimal requestedAmount, BigDecimal availableBalance) {
        super(String.format("Insufficient funds in account %s. Request : %s, Available :%s", accountNumber, requestedAmount, availableBalance),"INSUFFICIENT_FUNDS");
    }
}
