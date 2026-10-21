package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.RequestStatus;
import com.exercise.contactBook.model.*;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactChangeRequestRepository extends JpaRepository<ContactChangeRequest , Long> {
    List<ContactChangeRequest> findByOrganizationAndStatus(Organization organization , RequestStatus status);
    List<ContactChangeRequest> findByRequestedBy(User user);
    List<ContactChangeRequest> findByTargetContact(Contact targetContact);
    boolean existsByTargetContactAndStatus(Contact targetContact, RequestStatus status);


    void deleteByRequestedBy(User currentUser);

    long countByWorkflowAndStatus(ApprovalWorkflow workflow, RequestStatus status);

    @Modifying
    @Transactional
    @Query("update ContactChangeRequest r set r.workflow = null where r.workflow = :wf")
    void clearWorkflow(@Param("wf") ApprovalWorkflow wf);
}
