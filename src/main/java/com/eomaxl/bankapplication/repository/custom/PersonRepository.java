package com.eomaxl.bankapplication.repository.custom;

import com.eomaxl.bankapplication.domain.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository  extends JpaRepository<Person, Long> {
    Optional<Person> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT p FROM Person p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " + "OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Person> findByNameContaining(@Param("name") String name);

    @Query("SELECT p FROM Person p WHERE p.phoneNumber = :phoneNumber")
    Optional<Person> findByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT p FROM Person p WHERE LOWER(p.firstName) = LOWER(:firstName) " + "AND LOWER(p.lastName) = LOWER(:lastName)")
    List<Person> findByFirstNameAndLastNameIgnoreCase(@Param("firstName") String firstName, @Param("lastName") String lastName);
}
