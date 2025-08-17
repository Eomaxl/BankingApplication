package com.eomaxl.bankapplication.service;

import java.util.List;
import java.util.Optional;

public interface Person {

    Person createPerson(Person person);

    Optional<Person> findById(Long id);

    Optional<Person> findByEmail(String email);

    List<Person> findByName(String name);

    List<Person> findAll();

    Person updatePerson(Person person);

    void deletePerson(Long id);

    boolean existsByEmail(String email);
}
