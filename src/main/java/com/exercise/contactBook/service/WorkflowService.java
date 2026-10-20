package com.exercise.contactBook.service;

import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.enums.RequestStatus;
import com.exercise.contactBook.model.*;
import com.exercise.contactBook.repository.ApprovalPolicyRepository;
import com.exercise.contactBook.repository.ApprovalWorkflowRepository;
import com.exercise.contactBook.repository.ContactChangeRequestRepository;
import com.exercise.contactBook.repository.OrganizationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkflowService {
    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalPolicyRepository policyRepository;
    private final ContactChangeRequestRepository requestRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleService roleService;


    public WorkflowService(ApprovalWorkflowRepository workflowRepository,
                           ApprovalPolicyRepository policyRepository,
                           ContactChangeRequestRepository requestRepository,
                           OrganizationRepository organizationRepository,
                           RoleService roleService) {
        this.workflowRepository = workflowRepository;
        this.policyRepository = policyRepository;
        this.requestRepository = requestRepository;
        this.organizationRepository = organizationRepository;
        this.roleService = roleService;
    }


    /** The workflow this user's change must follow, or null when no policy applies. */
    public ApprovalWorkflow findFor(User user, ContactActionType action) {
        if (user.getOrganization() == null || user.getOrgRole() == null) return null;
        return policyRepository
                .findByOrganizationAndActionTypeAndRequesterRole(user.getOrganization(), action, user.getOrgRole())
                .map(ApprovalPolicy::getWorkflow)
                .orElse(null);
    }


    /** Gives an org its starting setup once: EMPLOYEE requests go to one MANAGER step. */
    @Transactional
    public void seedDefaults(Organization org) {
        if (Boolean.TRUE.equals(org.getWorkflowSeeded())) return;


        Map<String, OrgRole> roles = roleService.ensureDefaultRoles(org);


        ApprovalWorkflow wf = new ApprovalWorkflow();
        wf.setOrganization(org);
        wf.setName("Manager approval");
        ApprovalStep step = new ApprovalStep();
        step.setWorkflow(wf);
        step.setStepOrder(1);
        step.setApproverRole(roles.get("MANAGER"));
        wf.getSteps().add(step);
        wf = workflowRepository.save(wf);


        for (ContactActionType action : ContactActionType.values()) {
            ApprovalPolicy p = new ApprovalPolicy();
            p.setOrganization(org);
            p.setActionType(action);
            p.setRequesterRole(roles.get("EMPLOYEE"));
            p.setWorkflow(wf);
            policyRepository.save(p);
        }


        // requests created before workflows existed join the default workflow
        for (ContactChangeRequest r : requestRepository.findByOrganizationAndStatus(org, RequestStatus.PENDING)) {
            if (r.getWorkflow() == null) {
                r.setWorkflow(wf);
                r.setCurrentStep(1);
                requestRepository.save(r);
            }
        }
        org.setWorkflowSeeded(true);
        organizationRepository.save(org);
    }
}
