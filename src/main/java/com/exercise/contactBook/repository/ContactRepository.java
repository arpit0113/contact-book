package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.model.Contact;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {


    @Query("SELECT c FROM Contact c WHERE " +
            "((c.type = com.exercise.contactBook.enums.ContactType.PERSONAL AND c.user = :user)" +
            " OR (c.type = com.exercise.contactBook.enums.ContactType.COMMON AND c.organization = :organization))" +
            "AND (:type IS NULL OR c.type = :type)" +
            "AND (CAST(:search AS String) IS NULL OR " +
            "  LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%')) OR " +
            "  EXISTS (SELECT 1 FROM ContactPhone p WHERE p.contact = c AND LOWER(p.phone) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))) OR " +
            "  EXISTS (SELECT 1 FROM ContactEmail e WHERE e.contact = c AND LOWER(e.email) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))) )")
    List<Contact> findContactsFiltered(
            @Param("user") User user,
            @Param("organization") Organization organization,
            @Param("type") ContactType type,
            @Param("search") String search,
            Sort sort
    );


    void deleteByUserAndType(User currentUser, ContactType contactType);
}



