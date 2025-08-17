package com.eomaxl.bankapplication.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Entity
@Table( name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank( message = "Account number is required")
    @Column(name = "account_number", unique = false, nullable = false)
    private String accountNumber;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_holder_id", nullable = false)
    private AccountHolder accountHolder;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private final ReentrantReadWriteLock balanceLock = new ReentrantReadWriteLock();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null){
            status = AccountStatus.ACTIVE;
        }
        if (balance == null){
            balance = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public synchronized void credit(BigDecimal amount) {
        balanceLock.writeLock().lock();
        try {
            if(amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Credit Amount must be positive");
            }
            this.balance = this.balance.add(amount);
        } finally {
            balanceLock.writeLock().unlock();
        }
    }

    public synchronized void debit(BigDecimal amount) {
        balanceLock.writeLock().lock();
        try {
            if(amount.compareTo(BigDecimal.ZERO) <= 0){
                throw new IllegalArgumentException("Debit Amount must be positive");
            }
            if(this.balance.compareTo(amount) < 0) {
                throw new IllegalArgumentException("Insufficient Balance");
            }
            this.balance = this.balance.subtract(amount);
        } finally {
            balanceLock.writeLock().unlock();
        }
    }

    public BigDecimal getBalance(){
        balanceLock.readLock().lock();
        try {
            return balance;
        } finally {
            balanceLock.readLock().unlock();
        }
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transactions.setAccount(this);
    }

}
