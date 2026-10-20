package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.ApproveUserRequestDTO;
import com.exercise.contactBook.dto.PendingUserDTO;
import com.exercise.contactBook.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/pending-users")
    @PreAuthorize("hasAuthority('USER_APPROVE')")
    public ResponseEntity<List<PendingUserDTO>> getPendingUsers(){
        return ResponseEntity.ok(organizationService.getPendingUsers());
    }

    @PostMapping("/pending-users/{id}/approve")
    @PreAuthorize("hasAuthority('USER_APPROVE')")
    public ResponseEntity<Void> approveUser(@PathVariable Long id ,
                                            @Valid @RequestBody ApproveUserRequestDTO dto){
        organizationService.ApproveUser(id, dto);
        return  ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pending-users/{id}/reject")
    @PreAuthorize("hasAuthority('USER_APPROVE')")
    public ResponseEntity<Void> rejectUser(@PathVariable Long id){
        organizationService.rejectUser(id);
        return  ResponseEntity.noContent().build();
    }
}
