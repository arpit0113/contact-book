package com.exercise.contactBook.config;

import com.exercise.contactBook.enums.AuthProvider;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.enums.UserStatus;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SuperAdminConfig implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    public SuperAdminConfig(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if(userRepository.findByEmail(superAdminEmail).isPresent()){
            return;
        }
        User superAdmin = new User();
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setOrganization(null);
        superAdmin.setName("Super Admin");
        superAdmin.setEmail(superAdminEmail);
        superAdmin.setPassword(passwordEncoder.encode(superAdminPassword));
        superAdmin.setProvider(AuthProvider.LOCAL);
        superAdmin.setStatus(UserStatus.ACTIVE);

        userRepository.save(superAdmin);
    }
}
