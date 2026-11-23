package com.exercise.contactBook.security;

import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.service.CustomUserDetailsService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.AbstractAuthenticationToken;


@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserProvisioningService provisioningService;
    private final CustomUserDetailsService userDetailsService;

    public KeycloakJwtConverter(UserProvisioningService provisioningService,
                                CustomUserDetailsService userDetailsService) {
        this.provisioningService = provisioningService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new AccountAccessException("email_unavailable", 403);
        }
        if (!Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
            throw new AccountAccessException("email_not_verified", 403);
        }
        email = email.trim().toLowerCase();

        String sub = jwt.getSubject();
        String name = displayName(jwt, email);

        User user;
        try {
            user = provisioningService.resolve(sub, email, name);
        } catch (DataIntegrityViolationException e) {
            // Two first requests raced and the other one created the row. Read it back.
            user = provisioningService.resolve(sub, email, name);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountAccessException("pending_approval", 403);
        }

        UserDetails details = userDetailsService.build(user);
        return UsernamePasswordAuthenticationToken.authenticated(details, jwt, details.getAuthorities());
    }

    private static String displayName(Jwt jwt, String email) {
        for (String claim : new String[]{"name", "preferred_username"}) {
            String value = jwt.getClaimAsString(claim);
            if (value != null && !value.isBlank()) return value;
        }
        return email.substring(0, email.indexOf('@'));
    }
}