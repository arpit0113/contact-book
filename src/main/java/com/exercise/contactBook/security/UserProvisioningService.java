package com.exercise.contactBook.security;

import com.exercise.contactBook.enums.OrgRequestStatus;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.OrganizationCreationRequestRepository;
import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import com.exercise.contactBook.service.OrganizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
public class UserProvisioningService {
    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final OrganizationCreationRequestRepository requestRepository;

    public UserProvisioningService(UserRepository userRepository,
                                   OrganizationRepository organizationRepository,
                                   OrganizationService organizationService, OrganizationCreationRequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationService = organizationService;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public User resolve(String keycloakId, String email, String name) {
        Optional<User> bySub = userRepository.findByKeycloakId(keycloakId);
        if (bySub.isPresent()) {
            User user = bySub.get();
            if (!user.getEmail().equalsIgnoreCase(email)) {
                log.warn("Keycloak email changed for user {}: token has a different email than the database", user.getId());
            }
            return user;
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            if (user.getKeycloakId() != null && !user.getKeycloakId().equals(keycloakId)) {

                throw new AccountAccessException("account_conflict", 403);
            }
            user.setKeycloakId(keycloakId);
            return userRepository.save(user);
        }

        return createNew(keycloakId, email, name);
    }

    private User createNew(String keycloakId, String email, String name) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setName(name);

        if (organizationService.isFreeEmailDomain(email)) {
            user.setOrganization(null);
            user.setRole(Role.USER);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        }

        String domain = OrganizationService.extractDomain(email);
        Organization org = organizationRepository.findByAllowedDomain(domain).orElse(null);
        if (org == null) {
            throw new AccountAccessException(missingOrganizationCode(email, domain), 403);
        }

        user.setOrganization(org);
        user.setRole(null);
        user.setStatus(UserStatus.PENDING);
        return userRepository.save(user);
    }

    private String missingOrganizationCode(String email, String domain) {
        var own = requestRepository.findByAdminEmail(email);
        if (own.isPresent()) {
            return switch (own.get().getStatus()) {
                case PENDING_OTP -> "org_request_unverified";
                case PENDING_APPROVAL -> "org_request_pending";
                case REJECTED -> "org_request_rejected";
                default -> "no_organization_for_domain";
            };
        }

        var forDomain = requestRepository.findByOrgDomain(domain);
        if (forDomain.isPresent() && forDomain.get().getStatus() == OrgRequestStatus.PENDING_APPROVAL) {
            return "org_request_pending";
        }
        return "no_organization_for_domain";
    }
}
