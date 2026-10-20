package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.model.ContactEmail;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactEmailRepository extends JpaRepository<ContactEmail, Long> {
    Boolean existsByEmailAndContact_UserAndContact_Type(String email, User user, ContactType type);
    Boolean existsByEmailAndContact_OrganizationAndContact_Type(String email, Organization org , ContactType type);
    Boolean existsByEmailAndContact_UserAndContact_TypeAndContact_IdNot(String email, User user, ContactType type,Long contactId);
    Boolean existsByEmailAndContact_OrganizationAndContact_TypeAndContact_IdNot(String email, Organization org , ContactType type, Long contactId);

}
