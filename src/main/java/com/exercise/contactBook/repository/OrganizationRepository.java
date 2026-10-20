package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    @Query("SELECT o from Organization o JOIN o.allowedDomains d where LOWER(d) = LOWER(:domain)")
    Optional<Organization> findByAllowedDomain(@Param("domain") String domain);
}
