package com.eomaxl.bankapplication.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "transaction_id", unique = true, nullable = false)
    private String transactionId;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value="0.001", message ="Amount must be positive")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name="transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name="transaction_status")
    private TransactionStatus status;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="account_id",nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="target_account_id", nullable = false)
    private Account targetAccount;

    @Column(name = "balance_before", precision = 19, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if(transactionDate == null){
            transactionDate = LocalDateTime.now();
        }
        if(status == null){
            status = TransactionStatus.PENDING;
        }
    }

    public boolean isDebitTransaction() {
        return transactionType == TransactionType.DEBIT || transactionType == TransactionType.TRANSFER_OUT || transactionType == TransactionType.WITHDRAWAL;
    }

    public boolean isCreditTransaction() {
        return transactionType == TransactionType.CREDIT || transactionType == TransactionType.TRANSFER_IN || transactionType == TransactionType.DEPOSIT;
    }

}
