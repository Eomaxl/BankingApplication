package com.eomaxl.bankapplication.repository.custom.impl;

import com.eomaxl.bankapplication.domain.model.Account;
import com.eomaxl.bankapplication.domain.model.AccountStatus;
import com.eomaxl.bankapplication.domain.model.AccountType;
import com.eomaxl.bankapplication.repository.custom.CustomAccountRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomAccountRepositoryImpl implements CustomAccountRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Account> findAccountsWithComplexCriteria(String customerName, AccountType accountType, AccountStatus status, BigDecimal minBalance, BigDecimal maxBalance) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Account> criteria = builder.createQuery(Account.class);
        Root<Account> account = criteria.from(Account.class);

        List<Predicate> predicates = new ArrayList<>();

        if (customerName != null && !customerName.trim().isEmpty()){
            Join<Object, Object> accountHolder = account.join("accountHolder");
            Join<Object, Object> person = accountHolder.join("person");
            Predicate firstNamePredicate = builder.like(builder.lower(person.get("firstName")), "%" + customerName + "%");
            Predicate lastNamePredicate = builder.like(builder.lower(person.get("lastName")), "%" + customerName + "%");
            predicates.add(builder.or(firstNamePredicate, lastNamePredicate));
        }

        if (accountType != null) {
            predicates.add(builder.equal(account.get("accountType"), accountType));
        }

        if(status != null) {
            predicates.add(builder.equal(account.get("status"), status));
        }

        if(minBalance != null) {
            predicates.add(builder.greaterThanOrEqualTo(account.get("minBalance"), minBalance));
        }

        if(maxBalance != null) {
            predicates.add(builder.lessThanOrEqualTo(account.get("maxBalance"), maxBalance));
        }

        criteria.where(predicates.toArray(new Predicate[0]));
        criteria.orderBy(builder.desc(account.get("balance")));

        return entityManager.createQuery(criteria).getResultList();
    }

    @Override
    public Page<Account> searchAccounts(String searchTerm, Pageable pageable){
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Account> query = cb.createQuery(Account.class);
        Root<Account> account = query.from(Account.class);

        if(searchTerm != null && !searchTerm.trim().isEmpty()){
            Join<Object, Object> accountHolder = account.join("accountHolder");
            Join<Object, Object> person = accountHolder.join("person");
            Join<Object, Object> bank = account.join("bank");

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";

            Predicate accountNumberPredicate  = cb.like(cb.lower(account.get("accountNUmber")), searchPattern);
            Predicate customerIdPredicate = cb.like(cb.lower(accountHolder.get("customerId")), searchPattern);
            Predicate firstNamePredicate = cb.like(cb.lower(person.get("firstName")), searchPattern);
            Predicate lastNamePredicate = cb.like(cb.lower(person.get("lastName")), searchPattern);
            Predicate emailPredicate = cb.like(cb.lower(person.get("email")), searchPattern);
            Predicate bankNamePredicate = cb.like(cb.lower(bank.get("bankName")), searchPattern);

            query.where(cb.or(accountNumberPredicate, customerIdPredicate, firstNamePredicate, lastNamePredicate, emailPredicate, bankNamePredicate));
        }
        query.orderBy(cb.desc(account.get("createdAt")));

        TypedQuery<Account> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Account> results = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Account> countRoot = countQuery.from(Account.class);
        countQuery.select(cb.count(countRoot));

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Apply same predicates for count
            Join<Object, Object> countAccountHolder = countRoot.join("accountHolder");
            Join<Object, Object> countPerson = countAccountHolder.join("person");
            Join<Object, Object> countBank = countRoot.join("bank");

            String searchPattern = "%" + searchTerm.toLowerCase() + "%";

            Predicate accountNumberPredicate = cb.like(cb.lower(countRoot.get("accountNumber")), searchPattern);
            Predicate customerIdPredicate = cb.like(cb.lower(countAccountHolder.get("customerId")), searchPattern);
            Predicate firstNamePredicate = cb.like(cb.lower(countPerson.get("firstName")), searchPattern);
            Predicate lastNamePredicate = cb.like(cb.lower(countPerson.get("lastName")), searchPattern);
            Predicate emailPredicate = cb.like(cb.lower(countPerson.get("email")), searchPattern);
            Predicate bankNamePredicate = cb.like(cb.lower(countBank.get("bankName")), searchPattern);

            countQuery.where(cb.or(accountNumberPredicate, customerIdPredicate, firstNamePredicate,
                    lastNamePredicate, emailPredicate, bankNamePredicate));
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<Account> findDormantAccounts(LocalDateTime lastTransactionDate){
        String jpql = """
                SELECT DISTINCT a FROM Account a
                LEFT JOIN a.transactions t
                WHERE a.status = 'ACTIVE'
                AND (t.transactionDate IS NULL OR t.transactionDate < :lastTransactionDate)
                GROUP BY a.id
                HAVING MAX(t.transactionDate) < :lastTransactionDate OR MAX(t.transactionDate) IS NULL
                """;
        return entityManager.createQuery(jpql, Account.class)
                .setParameter("lastTransactionDate", lastTransactionDate)
                .getResultList();
    }

    @Override
    public List<Account> findHighValueAccounts(BigDecimal threshold){
        String jpql = """
            SELECT a FROM Account a 
            WHERE a.balance >= :threshold 
            AND a.status = 'ACTIVE'
            ORDER BY a.balance DESC
            """;

        return entityManager.createQuery(jpql, Account.class)
                .setParameter("threshold", threshold)
                .getResultList();
    }

    @Override
    public List<Object[]> getAccountSummaryByBank() {
        String jpql = """
            SELECT b.bankName, b.bankCode, 
                   COUNT(a), 
                   SUM(a.balance), 
                   AVG(a.balance),
                   MAX(a.balance),
                   MIN(a.balance)
            FROM Bank b 
            LEFT JOIN b.accounts a 
            WHERE a.status = 'ACTIVE' OR a.status IS NULL
            GROUP BY b.id, b.bankName, b.bankCode
            ORDER BY SUM(a.balance) DESC
            """;

        return entityManager.createQuery(jpql).getResultList();
    }
}
