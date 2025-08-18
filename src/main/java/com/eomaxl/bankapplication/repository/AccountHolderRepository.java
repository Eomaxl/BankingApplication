package com.eomaxl.bankapplication.repository;

import com.eomaxl.bankapplication.domain.model.AccountHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountHolderRepository extends JpaRepository<AccountHolder, Long> {

    Optional<AccountHolder> findByCustomerId(String customerId);

    boolean existsByCustomerId(String customerId);

    @Query("SELECT ah FROM AccountHolder ah WHERE ah.person.email = :email")
    Optional<AccountHolder> findByPersonEmail(@Param("email") String email);

    @Query("SELECT ah FROM AccountHolder ah WHERE ah.person.id = :personId")
    Optional<AccountHolder> findByPersonId(@Param("personId") Long personId);

    @Query("SELECT ah FROM AccountHolder ah WHERE ah.status = :status")
    List<AccountHolder> findByStatus(@Param("status") AccountHolder.AccountHolderStatus status);

    @Query("SELECT ah FROM AccountHolder ah JOIN FETCH ah.accounts WHERE ah.id = :accountHolderId")
    Optional<AccountHolder> findByIdWithAccounts(@Param("accountHolderId") Long accountHolderId);

    @Query("SELECT ah FROM AccountHolder ah JOIN FETCH ah.person WHERE ah.customerId = :customerId")
    Optional<AccountHolder> findByCustomerIdWithPerson(@Param("customerId") String customerId);

    @Query("SELECT COUNT(a) FROM AccountHolder ah JOIN ah.accounts a WHERE ah.id = :accountHolderId")
    Long countAccountsByAccountHolderId(@Param("accountHolderId") Long accountHolderId);
}
