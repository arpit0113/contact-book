package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.OrgRequestStatus;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.OrganizationCreationRequest;
import com.exercise.contactBook.model.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationCreationRequestRepository extends JpaRepository<OrganizationCreationRequest , Long> {
    List<OrganizationCreationRequest> findByStatus(OrgRequestStatus status);
    Optional<OrganizationCreationRequest> findByAdminEmail(String adminEmail);


    Optional<OrganizationCreationRequest> findByOrgDomain(String domain);

    @Modifying
    @Transactional
    @Query("update ContactChangeRequest r set r.reviewedBy = null where r.reviewedBy = :user")
    void clearReviewer(@Param("user") User user);
}
