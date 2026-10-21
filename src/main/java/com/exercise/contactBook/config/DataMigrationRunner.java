package com.exercise.contactBook.config;

import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import com.exercise.contactBook.service.RoleService;
import com.exercise.contactBook.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Order(2)
public class DataMigrationRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataMigrationRunner.class);


    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final WorkflowService workflowService;
    private final TransactionTemplate tx;


    public DataMigrationRunner(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               RoleService roleService,
                               WorkflowService workflowService,
                               PlatformTransactionManager transactionManager) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.workflowService = workflowService;
        this.tx = new TransactionTemplate(transactionManager);
    }


    @Override
    public void run(String... args) {
        for (Organization org : organizationRepository.findAll()) {
            try {
                tx.executeWithoutResult(s -> {
                    roleService.syncAdminPermissions(org);
                    workflowService.seedDefaults(org);
                });
            } catch (Exception ex) {
                log.error("Startup migration failed for organization {}: {}", org.getId(), ex.getMessage());
            }
        }
        backfillOrgRoles();
    }


    private void backfillOrgRoles() {
        tx.executeWithoutResult(s -> {
            int fixed = 0;
            for (User u : userRepository.findByOrgRoleIsNull()) {
                if (u.getOrganization() == null || u.getRole() == null) continue;
                if (u.getRole() == Role.USER || u.getRole() == Role.SUPER_ADMIN) continue;
                OrgRole role = roleService.forLegacyRole(u.getOrganization(), u.getRole());
                if (role != null) {
                    u.setOrgRole(role);
                    userRepository.save(u);
                    fixed++;
                }
            }
            if (fixed > 0) log.info("Backfilled orgRole for {} user(s)", fixed);
        });
    }
}