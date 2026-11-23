package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.OrgRequestStatus;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.OrganizationCreationRequest;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.OrganizationCreationRequestRepository;
import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import com.exercise.contactBook.security.KeycloakAdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OrganizationRequestService {
    private static final Logger log = LoggerFactory.getLogger(OrganizationRequestService.class);
    private final OrganizationCreationRequestRepository requestRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final OtpService otpService;
    private final MailService mailService;
    private final OrganizationService organizationService;
    private final OrganizationCreationRequestRepository organizationCreationRequestRepository;
    private final RoleService roleService;
    private final WorkflowService workflowService;
    public OrganizationRequestService(OrganizationCreationRequestRepository requestRepository,
                                      OrganizationRepository organizationRepository,
                                      UserRepository userRepository,
                                      KeycloakAdminClient keycloakAdminClient,
                                      OtpService otpService,
                                      MailService mailService,
                                      OrganizationService organizationService, OrganizationCreationRequestRepository organizationCreationRequestRepository, RoleService roleService, WorkflowService workflowService) {
        this.requestRepository = requestRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.keycloakAdminClient = keycloakAdminClient;
        this.otpService = otpService;
        this.mailService = mailService;
        this.organizationService = organizationService;
        this.organizationCreationRequestRepository = organizationCreationRequestRepository;
        this.roleService = roleService;
        this.workflowService = workflowService;
    }

    public Long submitRequest(OrgCreationRequestDTO dto){
        if(organizationService.isFreeEmailDomain(dto.getAdminEmail())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Please use a company email address");
        }
        String domain = OrganizationService.extractDomain(dto.getAdminEmail());
        if(organizationRepository.findByAllowedDomain(domain).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"An org is already exists for this domain - ask your admin to invite");
        }
        OrganizationCreationRequest creationRequest = organizationCreationRequestRepository.findByOrgDomain(domain).orElse(null);
        if(creationRequest != null) {
            if (creationRequest.getStatus() != OrgRequestStatus.REJECTED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A request for org creation for this domain already exists");
            }
        }
        if(userRepository.findByEmail(dto.getAdminEmail()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered - please log in");
        }
        OrganizationCreationRequest request = requestRepository.findByAdminEmail(dto.getAdminEmail())
                .orElse(new OrganizationCreationRequest());
        if(request.getId() != null) {
            switch (request.getStatus()){
                case PENDING_APPROVAL -> throw new ResponseStatusException(HttpStatus.CONFLICT, "Your request is already verified and waiting for admin's approval");
                case APPROVED -> throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered please log in");
                case PENDING_OTP -> {
                    if(request.getLastOtpSentAt() != null
                            && request.getLastOtpSentAt().isAfter(Instant.now().minus(60, ChronoUnit.SECONDS))){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "an otp just sent wait for a minute");
                    }
                }
                case REJECTED -> {
                    request.setReviewNote(null);
                    request.setReviewedBy(null);
                }
            }
        }
        request.setAdminName(dto.getAdminName());
        request.setAdminEmail(dto.getAdminEmail());
        request.setOrgDomain(domain);
        int dotIndex = domain.indexOf('.');
        request.setOrgName((dto.getOrgName() != null && !dto.getOrgName().isBlank())
                ?dto.getOrgName()
                :(dotIndex > 0  ? domain.substring(0,dotIndex) : domain));
        String otp = otpService.generateOtp();
        request.setOtp(otpService.hashOtp(otp));
        request.setOtpExpireAt(Instant.now().plus(3, ChronoUnit.MINUTES));
        request.setLastOtpSentAt(Instant.now());
        request.setOtpVerified(false);
        request.setStatus(OrgRequestStatus.PENDING_OTP);

        OrganizationCreationRequest saved = requestRepository.save(request);
        mailService.sendOtpEmail(dto.getAdminEmail(), otp);
        return saved.getId();
    }

    public void verifyOtp(OtpVerifyRequestDTO dto){

        OrganizationCreationRequest request = requestRepository.findById(Long.parseLong(dto.getRequestedId()))
                .orElseThrow(() ->  new ResourceNotFoundException("No organization found with this id"));

        if(request.getStatus() != OrgRequestStatus.PENDING_OTP){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "This request os not waiting for otp verification");

        }
        if(!otpService.matches(dto.getOtp(),request.getOtp())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Incorrect Otp");
        }
        if(request.getOtpExpireAt().isBefore(Instant.now())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST , "Otp expires - Resend OTP");
        }

        request.setOtpVerified(true);
        request.setStatus(OrgRequestStatus.PENDING_APPROVAL);
        requestRepository.save(request);
    }

    public List<OrgCreationRequestResponseDTO> getPendingRequests(){
        return requestRepository.findByStatus(OrgRequestStatus.PENDING_APPROVAL)
                .stream().map(this::toResponseDTO).toList();
    }

    public OrgCreationRequestResponseDTO getRequestDetails(Long requestId){
        return toResponseDTO(
                requestRepository.findById(requestId)
                        .orElseThrow(()->
                                new ResourceNotFoundException("resource not found with id : " + requestId)
                        )
        );
    }

    public void resendOtp(ResendRequestDTO dto){
        Long requestId = Long.parseLong(dto.getRequestId());
        String adminEmail = dto.getAdminEmail();
        OrganizationCreationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("No request found with this id: " + requestId));
        if(!request.getAdminEmail().equalsIgnoreCase(adminEmail)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Email does not match this request");
        }
        if(request.getStatus() != OrgRequestStatus.PENDING_OTP){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This request is not waiting for otp verification");
        }
        if(request.getLastOtpSentAt() != null
                &&request.getLastOtpSentAt().isAfter(Instant.now().minus(60, ChronoUnit.SECONDS))){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TO frequent try after 60 seconds");
        }
        String otp = otpService.generateOtp();
        request.setLastOtpSentAt(Instant.now());
        request.setOtpExpireAt(Instant.now().plus(3,ChronoUnit.MINUTES));
        request.setOtp(otpService.hashOtp(otp));
        requestRepository.save(request);
        mailService.sendOtpEmail(request.getAdminEmail(), otp);

    }

    @Transactional
    public void approveRequest(Long requestId){
        User superAdmin = getCurrentUser();
        OrganizationCreationRequest request = getPendingApprovalRequest(requestId);
        String domain = OrganizationService.extractDomain(request.getAdminEmail());
        if(organizationRepository.findByAllowedDomain(domain).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"An org is already exists for this domain - ask your admin to invite");
        }
        Organization org = new Organization();
        org.setName(request.getOrgName());
        org.getAllowedDomains().add(request.getOrgDomain());
        org.setDefaultRegion("IN");
        Organization savedOrg = organizationRepository.save(org);

        // The admin logs in through Keycloak. Reuse their Keycloak account if they already have
        // one (for example they signed in with Google), otherwise create it with a verified email
        // and let Keycloak email them a link to choose a password. If Keycloak fails, the exception
        // rolls this whole transaction back, so the request stays "pending approval" and approving
        // again is safe (an already created Keycloak user is found by email and reused).
        String keycloakId;
        boolean keycloakUserCreated = false;
        var existingKeycloakUser = keycloakAdminClient.findUserIdByEmail(request.getAdminEmail());
        if (existingKeycloakUser.isPresent()) {
            keycloakId = existingKeycloakUser.get();
        } else {
            keycloakId = keycloakAdminClient.createUser(request.getAdminEmail(), request.getAdminName());
            keycloakUserCreated = true;
        }

        User admin = new User();
        admin.setName(request.getAdminName());
        admin.setEmail(request.getAdminEmail());
        admin.setKeycloakId(keycloakId);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setOrganization(savedOrg);
        admin.setRole(Role.ADMIN);
        roleService.ensureDefaultRoles(savedOrg);
        workflowService.seedDefaults(savedOrg);
        admin.setOrgRole(roleService.forLegacyRole(savedOrg, Role.ADMIN));
        User savedAdmin = userRepository.save(admin);

        savedOrg.setAdmin(savedAdmin);
        organizationRepository.save(savedOrg);

        request.setStatus(OrgRequestStatus.APPROVED);
        request.setReviewedBy(superAdmin);

        requestRepository.save(request);

        if (keycloakUserCreated) {
            final String newKeycloakId = keycloakId;
            // Only email after the approval is really saved. If this fails the admin can still use
            // "Forgot password" on the Keycloak login page.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        keycloakAdminClient.sendSetPasswordEmail(newKeycloakId);
                    } catch (Exception e) {
                        log.warn("Could not send the set-password email for Keycloak user {}: {}", newKeycloakId, e.getMessage());
                    }
                }
            });
        }
    }


    public void rejectRequest(Long requestId, OrgRequestReviewDTO dto){
        User superAdmin = getCurrentUser();
        OrganizationCreationRequest request = getPendingApprovalRequest(requestId);
        request.setStatus(OrgRequestStatus.REJECTED);
        request.setReviewedBy(superAdmin);
        request.setReviewNote(dto!= null ? dto.getReviewNote() : null);
        requestRepository.save(request);
    }

    //helperrrrrrrr

    private OrganizationCreationRequest getPendingApprovalRequest(Long requestId){
        OrganizationCreationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request Not found with id: "+ requestId));
        if(request.getStatus() != OrgRequestStatus.PENDING_APPROVAL){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This request is not waiting for approval");
        }
        return request;
    }

    private OrgCreationRequestResponseDTO toResponseDTO(OrganizationCreationRequest request){
        OrgCreationRequestResponseDTO dto = new OrgCreationRequestResponseDTO();
        dto.setId(request.getId());
        dto.setAdminEmail(request.getAdminEmail());
        dto.setAdminName(request.getAdminName());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setOrgName(request.getOrgName());
        dto.setOrgDomain(request.getOrgDomain());
        dto.setReviewNote(request.getReviewNote());
        dto.setStatus(request.getStatus());
        return dto;
    }

    private User getCurrentUser(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("Logged in user not found"));
    }
}