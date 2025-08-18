package com.eomaxl.bankapplication.controller;

import com.eomaxl.bankapplication.dto.PersonDto;
import com.eomaxl.bankapplication.dto.response.ApiResponse;
import com.eomaxl.bankapplication.mapper.BankingMapper;
import com.eomaxl.bankapplication.service.IPersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Person Management", description = "APIs for managing person information")
public class PersonController {

    private final IPersonService personService;
    private final BankingMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new person", description = "Creates a new person record")
    public ResponseEntity<ApiResponse<PersonDto>> createPerson(@Valid @RequestBody PersonDto personDto) {
        log.info("Creating person with email: {}", personDto.getEmail());

        var person = mapper.toPerson(personDto);
        var savedPerson = personService.createPerson(person);
        var responseDto = mapper.toPersonDto(savedPerson);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Person created successfully", responseDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get person by ID", description = "Retrieves a person by their ID")
    public ResponseEntity<ApiResponse<PersonDto>> getPersonById(
            @Parameter(description = "Person ID") @PathVariable Long id) {
        log.info("Retrieving person with ID: {}", id);

        return personService.findById(id)
                .map(person -> {
                    var personDto = mapper.toPersonDto(person);
                    return ResponseEntity.ok(ApiResponse.success(personDto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get person by email", description = "Retrieves a person by their email address")
    public ResponseEntity<ApiResponse<PersonDto>> getPersonByEmail(
            @Parameter(description = "Email address") @PathVariable String email) {
        log.info("Retrieving person with email: {}", email);

        return personService.findByEmail(email)
                .map(person -> {
                    var personDto = mapper.toPersonDto(person);
                    return ResponseEntity.ok(ApiResponse.success(personDto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search persons by name", description = "Searches persons by first or last name")
    public ResponseEntity<ApiResponse<List<PersonDto>>> searchPersonsByName(
            @Parameter(description = "Name to search for") @RequestParam String name) {
        log.info("Searching persons by name: {}", name);

        var persons = personService.findByName(name);
        var personDtos = mapper.toPersonDtos(persons);

        return ResponseEntity.ok(ApiResponse.success(personDtos));
    }

    @GetMapping
    @Operation(summary = "Get all persons", description = "Retrieves all persons")
    public ResponseEntity<ApiResponse<List<PersonDto>>> getAllPersons() {
        log.info("Retrieving all persons");

        var persons = personService.findAll();
        var personDtos = mapper.toPersonDtos(persons);

        return ResponseEntity.ok(ApiResponse.success(personDtos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update person", description = "Updates an existing person")
    public ResponseEntity<ApiResponse<PersonDto>> updatePerson(
            @Parameter(description = "Person ID") @PathVariable Long id,
            @Valid @RequestBody PersonDto personDto) {
        log.info("Updating person with ID: {}", id);

        var person = mapper.toPerson(personDto);
        var updatedPerson = personService.updatePerson(id, person);
        var responseDto = mapper.toPersonDto(updatedPerson);

        return ResponseEntity.ok(ApiResponse.success("Person updated successfully", responseDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete person", description = "Deletes a person by ID")
    public ResponseEntity<ApiResponse<Void>> deletePerson(
            @Parameter(description = "Person ID") @PathVariable Long id) {
        log.info("Deleting person with ID: {}", id);

        personService.deletePerson(id);

        return ResponseEntity.ok(ApiResponse.success("Person deleted successfully", null));
    }
}