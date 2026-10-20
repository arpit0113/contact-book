package com.exercise.contactBook.config;

import com.exercise.contactBook.enums.AuthProvider;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import com.exercise.contactBook.service.CustomUserDetailsService;
import com.exercise.contactBook.service.OrganizationService;
import com.exercise.contactBook.service.RefreshTokenService;
import com.exercise.contactBook.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OauthSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final RefreshTokenService refreshTokenService;
    private final OrganizationRepository orgRepository;

    public OauthSuccessHandler(JwtUtil jwtUtil,
                               UserRepository userRepository,
                               OrganizationService organizationService, RefreshTokenService refreshTokenService, OrganizationRepository orgRepository){
        this.jwtUtil=jwtUtil;
        this.userRepository= userRepository;
        this.organizationService = organizationService;
        this.refreshTokenService = refreshTokenService;
        this.orgRepository = orgRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if(email == null || email.isBlank()){
            response.sendRedirect("http://localhost:4200/login?error=email_unavailable");
            return;
        }
        if(!Boolean.TRUE.equals(emailVerified)){
            response.sendRedirect("http://localhost:4200/login?error=email_not_verified");
        return;
        }

        email = email.toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if(user != null && user.getStatus() != UserStatus.ACTIVE){
            response.sendRedirect("http://localhost:4200/login?error=pending_approval");
            return;
        }

        if(user == null){
            if( organizationService.isFreeEmailDomain(email)){
               user = new User();
               user.setName(name != null && !name.isBlank() ? name : email.substring(0, email.indexOf('@')));
               user.setEmail(email);
               user.setProvider(AuthProvider.GOOGLE);
               user.setOrganization(null);
               user.setRole(Role.USER);
               user.setStatus(UserStatus.ACTIVE);
               userRepository.save(user);
            }else {
                String domain = OrganizationService.extractDomain(email);
                Organization org = orgRepository.findByAllowedDomain(domain).orElse(null);

                if (org == null) {
                    response.sendRedirect("http://localhost:4200/login?error=no_organization_for_domain");
                    return;
                }

                user = new User();
                user.setName(name != null && !name.isBlank() ? name : email.substring(0, email.indexOf('@')));
                user.setEmail(email.toLowerCase());
                user.setProvider(AuthProvider.GOOGLE);
                user.setOrganization(org);
                user.setRole(null);
                user.setStatus(UserStatus.PENDING);
                userRepository.save(user);
                response.sendRedirect("http://localhost:4200/login?error=pending_approval");
                return;
            }
        }
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = refreshTokenService.issueNewToken(user);
        response.sendRedirect("http://localhost:4200/oauth-success?token=" + accessToken + "&refreshToken=" + refreshToken);

    }
}
