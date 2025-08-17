package com.eomaxl.bankapplication.repository;

import com.eomaxl.bankapplication.domain.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank,Long> {
    Optional<Bank> findByBankCode(String bankCode);

    boolean existsByBankCode(String bankCode);

    @Query("SELECT b FROM Bank b WHERE LOWER(b.bankName) LIKE LOWER(CONCAT('%',:name, '%'))")
    List<Bank> findByBankNameContaining(@Param("name") String name);

    @Query("SELECT b FROM Bank b WHERE b.email = :email")
    Optional<Bank> findByEmail(String email);

    @Query("SELECT b from Bank b JOIN FETCH b.accounts WHERE b.id = :bankId")
    Optional<Bank> findByIdWithAccounts(@Param("bankId") Long bankId);

    @Query("SELECT COUNT(a) FROM Bank b JOIN b.accounts a WHERE b.id = :bankId")
    Long countAccountsByBankId(@Param("bankId") Long bankId);
}
