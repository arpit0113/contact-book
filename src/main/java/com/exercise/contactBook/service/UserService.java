package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.*;
import jakarta.transaction.Transactional;
import com.exercise.contactBook.security.KeycloakAdminClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final ContactChangeRequestRepository contactChangeRequestRepository;
    private final OrganizationCreationRequestRepository organizationCreationRequestRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;
    private final KeycloakAdminClient keycloakAdminClient;


    @Autowired
    public UserService(UserRepository userRepository,
                       ContactRepository contactRepository, ContactChangeRequestRepository contactChangeRequestRepository,
                       OrganizationCreationRequestRepository organizationCreationRequestRepository,
                       ApprovalPolicyRepository approvalPolicyRepository,
                       KeycloakAdminClient keycloakAdminClient){
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.contactChangeRequestRepository = contactChangeRequestRepository;
        this.organizationCreationRequestRepository = organizationCreationRequestRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    public UserResponseDTO getCurrentUserProfile(){
        return toUserResponseDTO(getCurrentUser());
    }


    public UserResponseDTO updateCurrentUser(UserUpdateRequestDTO dto){
        User existingUser = getCurrentUser();
        if (!existingUser.getEmail().equalsIgnoreCase(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Email cant be changed");
        }
        existingUser.setName(dto.getName());

        User savedUser =  userRepository.save(existingUser);

        return toUserResponseDTO(savedUser);

    }

    @Transactional
    public void deleteUser(){
        User currentUser = getCurrentUser();
        if(currentUser.getRole() == Role.SUPER_ADMIN){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The super admin account cant be deleted");
        }
        boolean isOrgAdmin = currentUser.getOrganization()!= null
                && currentUser.getOrganization().getAdmin()!= null
                &&   currentUser.getId().equals(currentUser.getOrganization().getAdmin().getId());
        if(isOrgAdmin){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete the organization's designated admin. Transfer ownership first");
        }
        contactChangeRequestRepository.deleteByRequestedBy(currentUser);
        contactChangeRequestRepository.clearReviewer(currentUser);
        organizationCreationRequestRepository.clearReviewer(currentUser);
        contactRepository.deleteByUserAndType(currentUser, ContactType.PERSONAL);
        userRepository.deleteById(currentUser.getId());
        // Also remove the login itself. If Keycloak fails, the exception rolls the database
        // changes back, so the person is not left half-deleted.
        if(currentUser.getKeycloakId() != null){
            keycloakAdminClient.deleteUser(currentUser.getKeycloakId());
        }
    }

    //helper methodssss

    public UserResponseDTO toUserResponseDTO(User user){
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(user.getId());
        responseDTO.setName(user.getName());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setOrganizationName(user.getOrganization()!= null ? user.getOrganization().getName() : "no_org");
        responseDTO.setRole(user.getOrgRole() != null ? user.getOrgRole().getName() :
                (user.getRole() == null ? null : user.getRole().name()));
        responseDTO.setPermissions(user.getOrgRole() == null ? List.of() : user.getOrgRole().getPermissions().stream().map(Enum::name).sorted().toList());
        responseDTO.setStatus(user.getStatus().name());
        responseDTO.setNeedsApproval(user.getOrgRole() != null && approvalPolicyRepository.existsByRequesterRole(user.getOrgRole()));
        responseDTO.setAccountType(user.getRole() == Role.SUPER_ADMIN ? "SUPER_ADMIN"
                :user.getOrganization() == null ? "PERSONAL" : "ORG_MEMBER");
        return responseDTO;
    }

    User getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));
    }


}