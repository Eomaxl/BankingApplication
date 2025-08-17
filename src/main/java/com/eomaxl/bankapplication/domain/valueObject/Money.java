package com.eomaxl.bankapplication.domain.valueObject;

import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

@Value
public class Money {
    BigDecimal amount;
    Currency currency;

    public Money(BigDecimal amount, Currency currency) {
        if(amount == null){
            throw new IllegalArgumentException("Amount cannot be null");
        }

        if(currency == null){
            throw new IllegalArgumentException("Currency cannot be null");
        }

        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public Money(String amount, String currencyCode) {
        this(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public Money add(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }

    public Money divide(BigDecimal divisor) {
        return new Money(this.amount.divide(divisor, currency.getDefaultFractionDigits(), RoundingMode.HALF_UP), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }

    private void validateSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot perform operation on different currencies");
        }
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money usd(String amount) {
        return new Money(amount, "USD");
    }

    public static Money usd(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("USD"));
    }

    @Override
    public String toString() {
        return currency.getSymbol() + amount.toString();
    }
}
