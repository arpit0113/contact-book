package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.OrgCreationRequestResponseDTO;
import com.exercise.contactBook.dto.OrgRequestReviewDTO;
import com.exercise.contactBook.service.OrganizationRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/organization-requests")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final OrganizationRequestService organizationRequestService;

    public SuperAdminController(OrganizationRequestService organizationRequestService) {
        this.organizationRequestService = organizationRequestService;
    }

    @GetMapping
    public ResponseEntity<List<OrgCreationRequestResponseDTO>> getPending() {
        return ResponseEntity.ok(organizationRequestService.getPendingRequests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrgCreationRequestResponseDTO> getOne(@PathVariable Long id){
        return ResponseEntity.ok(organizationRequestService.getRequestDetails(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        organizationRequestService.approveRequest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @RequestBody(required = false) OrgRequestReviewDTO dto) {
        organizationRequestService.rejectRequest(id, dto);
        return ResponseEntity.noContent().build();
    }
}
