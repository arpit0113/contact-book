package com.exercise.contactBook.repository;

import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByEmail(String email);
    List<User> findByOrganizationAndStatus(Organization organization, UserStatus status);
    List<User> findByOrgRoleIsNull();
    long countByOrgRole(OrgRole role);
}
