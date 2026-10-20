package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.ApproveUserRequestDTO;
import com.exercise.contactBook.dto.PendingUserDTO;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.OrgRoleRepository;
import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class OrganizationService {
    private final OrganizationRepository orgRepo;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final OrgRoleRepository orgRoleRepository;
    public OrganizationService(OrganizationRepository orgRepo, UserRepository userRepository, RoleService roleService, OrgRoleRepository orgRoleRepository){
        this.orgRepo = orgRepo;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.orgRoleRepository = orgRoleRepository;
    }

    public List<PendingUserDTO> getPendingUsers(){
        User admin = getCurrentUser();
        List<User> users = userRepository.findByOrganizationAndStatus(admin.getOrganization() , UserStatus.PENDING);
      return users.stream()
                .map(u -> new PendingUserDTO(u.getId(), u.getName(),u.getEmail()))
                .toList();
    }

    public void ApproveUser(Long userId, ApproveUserRequestDTO dto){
        User admin = getCurrentUser();
        OrgRole role = orgRoleRepository.findByIdAndOrganization(dto.getRoleId(),admin.getOrganization())
                .orElseThrow(() -> new ResourceNotFoundException("role not found"));

        if(role.getName().equalsIgnoreCase("ADMIN")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cannot assign admin role through approval.");
        }

        User pendingUser = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not exits with id:" + userId)
        );

        if(!pendingUser.getOrganization().getId().equals(admin.getOrganization().getId())){
            throw new ResourceNotFoundException("User not exits with id:" + userId);
        }
        if(pendingUser.getStatus() != UserStatus.PENDING){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User is not pending for approval - cannot approve");
        }
        pendingUser.setOrgRole(role);
        pendingUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(pendingUser);
    }

    public void rejectUser(Long userId){
        User admin = getCurrentUser();
        User pendingUser = userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User not exits with id:" + userId)
        );
        if(!pendingUser.getOrganization().getId().equals(admin.getOrganization().getId())){
            throw new ResourceNotFoundException("User not exits with id:" + userId);
        }
        if(pendingUser.getStatus() != UserStatus.PENDING){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"User is not pending for approval - cannot reject");
        }
        userRepository.deleteById(userId);

    }

    @Value("#{'${app.free-email-domains:gmail.com,yahoo.com,hotmail.com}'.split(',')}")
    private List<String> freeEmailDomains ;

    public static String extractDomain(String email){
        return email.substring(email.indexOf('@')+1).toLowerCase();
    }

    public boolean isFreeEmailDomain(String email){
        return freeEmailDomains.contains(extractDomain(email));
    }

    public record Assignment (Organization org , Role role , UserStatus status){};

    private User getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(
                () -> new ResourceNotFoundException("Logged-in user not found")
        );
    }
}
