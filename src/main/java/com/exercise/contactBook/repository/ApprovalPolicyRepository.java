package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.model.ApprovalPolicy;
import com.exercise.contactBook.model.ApprovalWorkflow;
import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalPolicyRepository extends JpaRepository<ApprovalPolicy , Long> {
    Optional<ApprovalPolicy> findByOrganizationAndActionTypeAndRequesterRole(
            Organization organization, ContactActionType actionType, OrgRole requesterRole
    );

    boolean existsByRequesterRole(OrgRole role);

    List<ApprovalPolicy> findByOrganization(Organization organization);

    boolean existsByWorkflow(ApprovalWorkflow workflow);

}
