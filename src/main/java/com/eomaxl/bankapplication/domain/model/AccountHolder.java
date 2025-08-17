package com.eomaxl.bankapplication.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "account_holders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountHolder {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "customer_id", unique = true, nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name="account_holder_status")
    private AccountHolderStatus status;

    @OneToMany(mappedBy = "accountHolder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @Column(name ="created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addAccount(Account account) {
        accounts.add(account);
        account.setAccountHolder(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setAccountHolder(null);
    }

    public enum AccountHolderStatus {
        ACTIVE, INACTIVE, SUSPENDED, CLOSED
    }
}
