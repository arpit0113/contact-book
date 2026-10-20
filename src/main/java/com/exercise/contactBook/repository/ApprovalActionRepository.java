package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction , Long> {
}
