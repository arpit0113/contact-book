package com.exercise.contactBook.config;

import com.exercise.contactBook.repository.OrganizationRepository;
import com.exercise.contactBook.repository.UserRepository;
import com.exercise.contactBook.service.RoleService;
import com.exercise.contactBook.service.WorkflowService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminPermissionSync implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final RoleService roleService;
    private final WorkflowService workflowService;

    public AdminPermissionSync(OrganizationRepository organizationRepository,
                               RoleService roleService, WorkflowService workflowService) {
        this.organizationRepository = organizationRepository;
        this.roleService = roleService;
        this.workflowService = workflowService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        organizationRepository.findAll().forEach(org ->{
            roleService.syncAdminPermissions(org);
            workflowService.seedDefaults(org);
        });

    }
}