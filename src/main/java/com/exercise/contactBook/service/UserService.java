package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.AuthProvider;
import com.exercise.contactBook.enums.ContactType;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.*;
import com.exercise.contactBook.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OrganizationService organizationService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ContactRepository contactRepository;
    private final ContactChangeRequestRepository contactChangeRequestRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationCreationRequestRepository organizationCreationRequestRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;


    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OrganizationService organizationService,
                       RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository,
                       ContactRepository contactRepository, ContactChangeRequestRepository contactChangeRequestRepository,
                       OrganizationRepository organizationRepository,
                       OrganizationCreationRequestRepository organizationCreationRequestRepository, ApprovalPolicyRepository approvalPolicyRepository){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.organizationService = organizationService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.contactRepository = contactRepository;
        this.contactChangeRequestRepository = contactChangeRequestRepository;
        this.organizationRepository = organizationRepository;
        this.organizationCreationRequestRepository = organizationCreationRequestRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
    }

    public String generateToken(User user){
        return jwtUtil.generateToken(user.getEmail());
    }

    public AuthResponseDTO registerUser(UserRequestDTO dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"email already registered");
        }
        if(organizationService.isFreeEmailDomain(dto.getEmail())){
            User user = new User();
            user.setRole(Role.USER);
            user.setStatus(UserStatus.ACTIVE);
            user.setOrganization(null);
            user.setName(dto.getName());
            user.setEmail(dto.getEmail());
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setProvider(AuthProvider.LOCAL);
            User savedUser = userRepository.save(user);

            UserResponseDTO userResponseDTO =  toUserResponseDTO(savedUser);
            TokenPairResponseDTO tokens = issueToken(savedUser);
            return new AuthResponseDTO("ACTIVE", tokens.getAccessToken(),tokens.getRefreshToken(), "Account created.",userResponseDTO);

        }

        String domain = OrganizationService.extractDomain(dto.getEmail());
        Organization org = organizationRepository.findByAllowedDomain(domain)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No organization exists for this email domain yet. " +
                                "Ask your admin for an invite, or request a new organization."));


        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setOrganization(org);
        user.setRole(null);
        user.setStatus(UserStatus.PENDING);
        User savedUser = userRepository.save(user);

        UserResponseDTO userResponseDTO =  toUserResponseDTO(savedUser);
            return new AuthResponseDTO(
                    "PENDING", null, null, "Your account is created and is in pending for your organizations admin's approval",
                    userResponseDTO );
    }

    public TokenPairResponseDTO loginUser(LoginRequestDTO dto) {

        explainPendingOrganizationRequest(dto);
            User user = userRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if(user.getPassword() == null || !passwordEncoder.matches(dto.getPassword() , user.getPassword())){
                throw new BadCredentialsException("Invalid email or password");
            }

            if (UserStatus.PENDING == user.getStatus()) {
                throw new DisabledException("Your account is pending approval from your organization's admin.");
            }

            return issueToken(user);

    }

    private void explainPendingOrganizationRequest(LoginRequestDTO dto){
        organizationCreationRequestRepository.findByAdminEmail(dto.getEmail())
                .ifPresent(request ->{
                    if(!passwordEncoder.matches(dto.getPassword(), request.getPassword())) throw new BadCredentialsException("Invalid email or password");
                    switch (request.getStatus()){
                        case PENDING_OTP -> throw  new ResponseStatusException(HttpStatus.FORBIDDEN , "Your organization request is not verified yet - open register an organization  and enter otp sent to your email");
                        case PENDING_APPROVAL -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "your organization request is waiting for admin's approval");
                        case REJECTED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "your organization request was rejected" + (request.getReviewNote() != null && !request.getReviewNote().isBlank() ? ":" + request.getReviewNote() : "") + ". You can fix the details and apply again from 'register an organization'.");
                        default -> { }
                    }
                } );
    }

    public TokenPairResponseDTO issueToken(User user){

        String accessToken = generateToken(user);
        String refreshToken = refreshTokenService.issueNewToken(user);

        return new TokenPairResponseDTO(accessToken,refreshToken);
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
        refreshTokenRepository.deleteByUser(currentUser);
        contactChangeRequestRepository.deleteByRequestedBy(currentUser);
        contactChangeRequestRepository.clearReviewer(currentUser);
        organizationCreationRequestRepository.clearReviewer(currentUser);
        contactRepository.deleteByUserAndType(currentUser, ContactType.PERSONAL);
        userRepository.deleteById(currentUser.getId());
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
