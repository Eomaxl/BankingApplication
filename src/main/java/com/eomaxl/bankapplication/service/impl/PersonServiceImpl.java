package com.eomaxl.bankapplication.service.impl;

import com.eomaxl.bankapplication.domain.exception.BankingException;
import com.eomaxl.bankapplication.domain.model.Person;
import com.eomaxl.bankapplication.repository.PersonRepository;
import com.eomaxl.bankapplication.service.IPersonService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PersonServiceImpl implements IPersonService {
    private PersonRepository personRepository;

    @Transactional
    public Person createPerson(Person person) {
        log.info("Creating a new person with email: {}",person.getEmail());

        if(personRepository.existsByEmail(person.getEmail())) {
            throw new BankingException("Person with email already exists: "+person.getEmail(),"DUPLICATE_EMAIL");
        }

        Person savedPerson = personRepository.save(person);
        log.info("Successfully created person with ID: {}",savedPerson.getId());
        return savedPerson;
    }

    public Optional<Person> findById(Long id){
        log.debug("Finding person with ID: {}",id);
        return personRepository.findById(id);
    }

    public Optional<Person> findByEmail(String email){
        log.debug("Finding person with email: {}",email);
        return personRepository.findByEmail(email);
    }

    public List<Person> findByName(String name) {
        log.debug("Searching persons by name: {}", name);
        return personRepository.findByNameContaining(name);
    }

    public List<Person> findAll() {
        log.debug("Retrieving all persons");
        return personRepository.findAll();
    }

    @Transactional
    public Person updatePerson(Long id, Person updatedPerson) {
        log.info("Updating person with ID: {}", id);

        Person existingPerson = personRepository.findById(id)
                .orElseThrow(() -> new BankingException("Person not found with ID: " + id, "PERSON_NOT_FOUND"));

        // Check if email is being changed and if it's already taken
        if (!existingPerson.getEmail().equals(updatedPerson.getEmail()) &&
                personRepository.existsByEmail(updatedPerson.getEmail())) {
            throw new BankingException("Email already exists: " + updatedPerson.getEmail(), "DUPLICATE_EMAIL");
        }

        existingPerson.setFirstName(updatedPerson.getFirstName());
        existingPerson.setLastName(updatedPerson.getLastName());
        existingPerson.setEmail(updatedPerson.getEmail());
        existingPerson.setPhoneNumber(updatedPerson.getPhoneNumber());
        existingPerson.setAddress(updatedPerson.getAddress());
        existingPerson.setDateOfBirth(updatedPerson.getDateOfBirth());

        Person savedPerson = personRepository.save(existingPerson);
        log.info("Successfully updated person with ID: {}", savedPerson.getId());
        return savedPerson;
    }

    @Transactional
    public void deletePerson(Long id) {
        log.info("Deleting person with ID: {}", id);

        if (!personRepository.existsById(id)) {
            throw new BankingException("Person not found with ID: " + id, "PERSON_NOT_FOUND");
        }

        personRepository.deleteById(id);
        log.info("Successfully deleted person with ID: {}", id);
    }

    public boolean existsByEmail(String email) {
        return personRepository.existsByEmail(email);
    }
}
