package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.ContactChangeRequestDTO;
import com.exercise.contactBook.dto.ContactResponseDTO;
import com.exercise.contactBook.dto.ReviewRequestDTO;
import com.exercise.contactBook.service.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contact-requests")
public class ContactRequestController {
    private final ContactService contactService;

    public ContactRequestController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('REQUEST_APPROVE')")
    public ResponseEntity<List<ContactChangeRequestDTO>> getPendingRequests(){
        return ResponseEntity.status(HttpStatus.OK).body(contactService.getPendingRequests());
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ContactChangeRequestDTO>> getMineRequests(){
        return ResponseEntity.ok(contactService.getMyRequest());
    }

    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAuthority('REQUEST_APPROVE')")
    public ResponseEntity<ContactResponseDTO> approve(@PathVariable Long requestId){
        return ResponseEntity.ok(contactService.approveRequest(requestId));
    }

    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAuthority('REQUEST_APPROVE')")
    public ResponseEntity<Void> reject(@PathVariable Long requestId , @RequestBody(required = false) ReviewRequestDTO dto){
        contactService.rejectRequest(requestId, dto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> cancel(@PathVariable Long requestId){
        contactService.cancelRequest(requestId);
        return ResponseEntity.noContent().build();
    }
}
