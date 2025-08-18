package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Person;

import java.util.List;
import java.util.Optional;

public interface PersonService {

    Person createPerson(Person person);

    Optional<Person> findById(Long id);

    Optional<Person> findByEmail(String email);

    List<Person> findByName(String name);

    List<Person> findAll();

    Person updatePerson(Long id, Person updatedPerson);

    void deletePerson(Long id);

    boolean existsByEmail(String email);
}
