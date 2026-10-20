package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.model.ContactPhone;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactPhoneRepository extends JpaRepository<ContactPhone, Long> {
    Boolean existsByPhoneAndContact_UserAndContact_Type(String phone, User user, ContactType type);
    Boolean existsByPhoneAndContact_OrganizationAndContact_Type(String phone, Organization org , ContactType type);
    Boolean existsByPhoneAndContact_UserAndContact_TypeAndContact_IdNot(String phone, User user, ContactType type,Long contactId);
    Boolean existsByPhoneAndContact_OrganizationAndContact_TypeAndContact_IdNot(String phone, Organization org , ContactType type, Long contactId);
}
