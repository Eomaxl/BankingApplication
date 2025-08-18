package com.eomaxl.bankapplication.service;

import com.eomaxl.bankapplication.domain.model.Person;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for Person management operations
 * Provides contract for person-related business logic
 */
public interface IPersonService {

    /**
     * Creates a new person in the system
     * @param person Person entity to create
     * @return Created person with generated ID
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if email already exists
     */
    Person createPerson(Person person);

    /**
     * Finds a person by their unique ID
     * @param id Person ID
     * @return Optional containing person if found
     */
    Optional<Person> findById(Long id);

    /**
     * Finds a person by their email address
     * @param email Email address
     * @return Optional containing person if found
     */
    Optional<Person> findByEmail(String email);

    /**
     * Searches for persons by name (first or last name)
     * @param name Name to search for (case-insensitive partial match)
     * @return List of matching persons
     */
    List<Person> findByName(String name);

    /**
     * Retrieves all persons in the system
     * @return List of all persons
     */
    List<Person> findAll();

    /**
     * Updates an existing person's information
     * @param id Person ID to update
     * @param updatedPerson Updated person data
     * @return Updated person entity
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if person not found or email conflict
     */
    Person updatePerson(Long id, Person updatedPerson);

    /**
     * Deletes a person from the system
     * @param id Person ID to delete
     * @throws com.eomaxl.bankapplication.domain.exception.BankingException if person not found
     */
    void deletePerson(Long id);

    /**
     * Checks if a person exists with the given email
     * @param email Email address to check
     * @return true if person exists with this email
     */
    boolean existsByEmail(String email);
}
