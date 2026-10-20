package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.RequestStatus;
import com.exercise.contactBook.model.Contact;
import com.exercise.contactBook.model.ContactChangeRequest;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactChangeRequestRepository extends JpaRepository<ContactChangeRequest , Long> {
    List<ContactChangeRequest> findByOrganizationAndStatus(Organization organization , RequestStatus status);
    List<ContactChangeRequest> findByRequestedBy(User user);
    List<ContactChangeRequest> findByTargetContact(Contact targetContact);
    boolean existsByTargetContactAndStatus(Contact targetContact, RequestStatus status);


    void deleteByRequestedBy(User currentUser);
}
