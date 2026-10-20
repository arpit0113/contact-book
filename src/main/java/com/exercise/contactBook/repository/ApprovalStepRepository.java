package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.ApprovalStep;
import com.exercise.contactBook.model.OrgRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
    boolean existsByApproverRole(OrgRole role);
}
