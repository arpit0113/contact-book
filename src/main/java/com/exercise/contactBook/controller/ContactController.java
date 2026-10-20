package com.exercise.contactBook.controller;
import com.exercise.contactBook.dto.*;

import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public ResponseEntity<List<ContactResponseDTO>> getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ContactType type,
            Sort sort) { // Automatically parses structures like ?sort=name,asc or ?sort=createdAt,desc

        if (sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.ASC, "name");
        }

        List<ContactResponseDTO> results = contactService.getSearchedAndSortedContacts(
                search, sort , type
        );
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactResponseDTO> getContactById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(contactService.getContactById(id));
    }


    @PostMapping("")
    public ResponseEntity<ContactWriteResultDTO> addContact(@Valid @RequestBody ContactRequestDTO dto) {
        ContactWriteResultDTO result = contactService.addContact(dto);
        return ResponseEntity.status(result.isPendingApproval() ? HttpStatus.ACCEPTED : HttpStatus.CREATED)
                .body(result);
    }



    @PostMapping(value = "/import-encrypted")
    public ResponseEntity<BulkImportResultDTO> importEncryptedCsv(@Valid @RequestBody EncryptedCSVRequestDTO dto){
        return ResponseEntity.ok(contactService.importEncryptedCsv(dto));
    }



    @PutMapping("/{id}")
    public ResponseEntity<ContactWriteResultDTO> updateContact(@PathVariable Long id , @Valid @RequestBody ContactRequestDTO dto){
       ContactWriteResultDTO result = contactService.updateContact(id , dto);
        return ResponseEntity.status(result.isPendingApproval() ? HttpStatus.ACCEPTED : HttpStatus.OK)
                .body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
        return ResponseEntity.noContent().build();

    }
}
