package com.eomaxl.bankapplication.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "banks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bank name is required")
    @Size(max = 100)
    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @NotBlank(message = "Bank code is required")
    @Size(max = 10)
    @Column(name = "bank_code", unique = true, nullable = false)
    private String bankCode;

    @Size(max = 200)
    private String address;

    @Size(max = 15)
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 100)
    private String email;

    @OneToMany(mappedBy = "bank", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
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
        account.setBank(this);
    }

    public void removeAccount(Account account) {
        accounts.remove(account);
        account.setBank(null);
    }
}
