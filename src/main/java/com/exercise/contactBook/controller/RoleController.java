package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.PermissionDTO;
import com.exercise.contactBook.dto.RoleDTO;
import com.exercise.contactBook.dto.RoleRequestDTO;
import com.exercise.contactBook.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

        private final RoleService roleService;

        public RoleController(RoleService roleService) { this.roleService = roleService; }

        // the pending-users page also needs this list, hence USER_APPROVE
        @GetMapping
        @PreAuthorize("hasAnyAuthority('ROLE_MANAGE','USER_APPROVE')")
        public ResponseEntity<List<RoleDTO>> list() {
            return ResponseEntity.ok(roleService.listRoles());
        }

        @GetMapping("/permissions")
        @PreAuthorize("hasAuthority('ROLE_MANAGE')")
        public ResponseEntity<List<PermissionDTO>> permissions() {
            return ResponseEntity.ok(roleService.listPermissions());
        }

        @PostMapping
        @PreAuthorize("hasAuthority('ROLE_MANAGE')")
        public ResponseEntity<RoleDTO> create(@Valid @RequestBody RoleRequestDTO dto) {
            return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(dto));
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('ROLE_MANAGE')")
        public ResponseEntity<RoleDTO> update(@PathVariable Long id, @Valid @RequestBody RoleRequestDTO dto) {
            return ResponseEntity.ok(roleService.updateRole(id, dto));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('ROLE_MANAGE')")
        public ResponseEntity<Void> delete(@PathVariable Long id) {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        }
    }

