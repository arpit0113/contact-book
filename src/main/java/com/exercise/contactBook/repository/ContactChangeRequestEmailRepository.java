package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.RequestStatus;
import com.exercise.contactBook.model.ContactChangeRequestEmail;
import com.exercise.contactBook.model.ContactChangeRequestPhone;
import com.exercise.contactBook.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactChangeRequestEmailRepository extends JpaRepository<ContactChangeRequestEmail, Long> {
    boolean existsByEmailAndChangeRequest_OrganizationAndChangeRequestStatus(
            String phone, Organization org, RequestStatus status
            );

}
