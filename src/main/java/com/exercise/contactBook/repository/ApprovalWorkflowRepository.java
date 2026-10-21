package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.ApprovalWorkflow;
import com.exercise.contactBook.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    List<ApprovalWorkflow> findByOrganization(Organization organization);
    Optional<ApprovalWorkflow> findByIdAndOrganization(Long id, Organization organization);
}
