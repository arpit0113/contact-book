package com.exercise.contactBook.repository;

import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgRoleRepository extends JpaRepository<OrgRole, Long> {
    List<OrgRole> findByOrganization(Organization organization);
    Optional<OrgRole> findByOrganizationAndNameIgnoreCase(Organization organization, String name);
    Optional<OrgRole> findByIdAndOrganization(Long id, Organization organization);
}
