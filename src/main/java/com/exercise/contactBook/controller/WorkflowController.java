package com.exercise.contactBook.controller;

import com.exercise.contactBook.dto.PolicyDTO;
import com.exercise.contactBook.dto.PolicyRequestDTO;
import com.exercise.contactBook.dto.WorkflowDTO;
import com.exercise.contactBook.dto.WorkflowRequestDTO;
import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.security.WorkflowManage;
import com.exercise.contactBook.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final WorkflowService workflowService;


    public WorkflowController(WorkflowService workflowService) { this.workflowService = workflowService; }


    @GetMapping
    @WorkflowManage
    public ResponseEntity<List<WorkflowDTO>> list() { return ResponseEntity.ok(workflowService.listWorkflows()); }


    @PostMapping
    @WorkflowManage
    public ResponseEntity<WorkflowDTO> create(@Valid @RequestBody WorkflowRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.createWorkflow(dto));
    }


    @PutMapping("/{id}")
    @WorkflowManage
    public ResponseEntity<WorkflowDTO> update(@PathVariable Long id, @Valid @RequestBody WorkflowRequestDTO dto) {
        return ResponseEntity.ok(workflowService.updateWorkflow(id, dto));
    }


    @DeleteMapping("/{id}")
    @WorkflowManage
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/policies")
    @WorkflowManage
    public ResponseEntity<List<PolicyDTO>> policies() { return ResponseEntity.ok(workflowService.listPolicies()); }


    @PutMapping("/policies")
    @WorkflowManage
    public ResponseEntity<PolicyDTO> setPolicy(@Valid @RequestBody PolicyRequestDTO dto) {
        return ResponseEntity.ok(workflowService.setPolicy(dto));
    }


    @DeleteMapping("/policies")
    @WorkflowManage
    public ResponseEntity<Void> clearPolicy(@RequestParam ContactActionType actionType, @RequestParam Long roleId) {
        workflowService.clearPolicy(actionType, roleId);
        return ResponseEntity.noContent().build();
    }
}
