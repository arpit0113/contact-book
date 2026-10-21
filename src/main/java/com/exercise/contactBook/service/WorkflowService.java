package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.*;
import com.exercise.contactBook.enums.ApprovalMode;
import com.exercise.contactBook.enums.ContactActionType;
import com.exercise.contactBook.enums.Permission;
import com.exercise.contactBook.enums.RequestStatus;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.*;
import com.exercise.contactBook.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class WorkflowService {
    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalPolicyRepository policyRepository;
    private final ContactChangeRequestRepository requestRepository;
    private final OrganizationRepository organizationRepository;
    private final RoleService roleService;
    private final OrgRoleRepository orgRoleRepository;
    private final UserRepository userRepository;


    public WorkflowService(ApprovalWorkflowRepository workflowRepository,
                           ApprovalPolicyRepository policyRepository,
                           ContactChangeRequestRepository requestRepository,
                           OrganizationRepository organizationRepository,
                           RoleService roleService, OrgRoleRepository orgRoleRepository, UserRepository userRepository) {
        this.workflowRepository = workflowRepository;
        this.policyRepository = policyRepository;
        this.requestRepository = requestRepository;
        this.organizationRepository = organizationRepository;
        this.roleService = roleService;
        this.orgRoleRepository = orgRoleRepository;
        this.userRepository = userRepository;
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

    public List<WorkflowDTO> listWorkflows() {
        Organization org = currentOrg();
        return workflowRepository.findByOrganization(org).stream()
                .sorted(Comparator.comparing(ApprovalWorkflow::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto).toList();
    }


    @Transactional
    public WorkflowDTO createWorkflow(WorkflowRequestDTO dto) {
        Organization org = currentOrg();
        String name = dto.getName().trim();
        assertNameFree(org, name, null);
        List<OrgRole> roles = resolveRoles(org, dto.getRoleIds());


        ApprovalWorkflow wf = new ApprovalWorkflow();
        wf.setOrganization(org);
        wf.setName(name);
        wf.setMode(dto.getMode());
        fillSteps(wf, roles);
        return toDto(workflowRepository.save(wf));
    }


    @Transactional
    public WorkflowDTO updateWorkflow(Long id, WorkflowRequestDTO dto) {
        Organization org = currentOrg();
        ApprovalWorkflow wf = workflowRepository.findByIdAndOrganization(id, org)
                .orElseThrow(() -> new ResourceNotFoundException("Flow not found"));
        assertNotInProgress(wf);
        String name = dto.getName().trim();
        assertNameFree(org, name, id);
        List<OrgRole> roles = resolveRoles(org, dto.getRoleIds());


        wf.setName(name);
        wf.setMode(dto.getMode());
        fillSteps(wf, roles);
        return toDto(workflowRepository.save(wf));
    }


    @Transactional
    public void deleteWorkflow(Long id) {
        Organization org = currentOrg();
        ApprovalWorkflow wf = workflowRepository.findByIdAndOrganization(id, org)
                .orElseThrow(() -> new ResourceNotFoundException("Flow not found"));
        assertNotInProgress(wf);
        if (policyRepository.existsByWorkflow(wf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Some roles still use this flow. Point them to another flow first");
        }
        requestRepository.clearWorkflow(wf);   // old finished requests keep their history
        workflowRepository.delete(wf);
    }


    public List<PolicyDTO> listPolicies() {
        return policyRepository.findByOrganization(currentOrg()).stream()
                .map(this::toPolicyDto).toList();
    }


    @Transactional
    public PolicyDTO setPolicy(PolicyRequestDTO dto) {
        Organization org = currentOrg();
        OrgRole role = orgRoleRepository.findByIdAndOrganization(dto.getRequesterRoleId(), org)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        if (role.getName().equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The ADMIN role can't have an approval policy");
        }
        ApprovalWorkflow wf = workflowRepository.findByIdAndOrganization(dto.getWorkflowId(), org)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Flow not found"));


        ApprovalPolicy policy = policyRepository
                .findByOrganizationAndActionTypeAndRequesterRole(org, dto.getActionType(), role)
                .orElseGet(ApprovalPolicy::new);
        policy.setOrganization(org);
        policy.setActionType(dto.getActionType());
        policy.setRequesterRole(role);
        policy.setWorkflow(wf);
        return toPolicyDto(policyRepository.save(policy));
    }


    @Transactional
    public void clearPolicy(ContactActionType action, Long roleId) {
        Organization org = currentOrg();
        OrgRole role = orgRoleRepository.findByIdAndOrganization(roleId, org)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        policyRepository.findByOrganizationAndActionTypeAndRequesterRole(org, action, role)
                .ifPresent(policyRepository::delete);
    }



    private List<OrgRole> resolveRoles(Organization org, List<Long> roleIds) {
        if (new HashSet<>(roleIds).size() != roleIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each role can appear only once in a flow");
        }
        List<OrgRole> roles = new ArrayList<>();
        for (Long id : roleIds) {
            OrgRole role = orgRoleRepository.findByIdAndOrganization(id, org)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
            if (!role.getPermissions().contains(Permission.REQUEST_APPROVE)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The role " + role.getName() + " can't review requests. Give it that permission first");
            }
            roles.add(role);
        }
        return roles;
    }


    private void fillSteps(ApprovalWorkflow wf, List<OrgRole> roles) {
        wf.getSteps().clear();
        int order = 1;
        for (OrgRole role : roles) {
            ApprovalStep step = new ApprovalStep();
            step.setWorkflow(wf);
            step.setStepOrder(order++);
            step.setApproverRole(role);
            wf.getSteps().add(step);
        }
    }


    private void assertNameFree(Organization org, String name, Long exceptId) {
        boolean taken = workflowRepository.findByOrganization(org).stream()
                .anyMatch(w -> w.getName().equalsIgnoreCase(name) && !w.getId().equals(exceptId));
        if (taken) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A flow with this name already exists");
        }
    }


    private void assertNotInProgress(ApprovalWorkflow wf) {
        long n = requestRepository.countByWorkflowAndStatus(wf, RequestStatus.PENDING);
        if (n > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    n + " request(s) are still waiting on this flow. Finish or reject them first");
        }
    }


    private WorkflowDTO toDto(ApprovalWorkflow wf) {
        List<WorkflowStepDTO> steps = wf.getSteps().stream()
                .map(s -> new WorkflowStepDTO(s.getStepOrder(), s.getApproverRole().getId(), s.getApproverRole().getName()))
                .toList();
        return new WorkflowDTO(wf.getId(), wf.getName(),
                wf.getMode() == null ? ApprovalMode.SEQUENTIAL : wf.getMode(),
                steps, requestRepository.countByWorkflowAndStatus(wf, RequestStatus.PENDING));
    }


    private PolicyDTO toPolicyDto(ApprovalPolicy p) {
        return new PolicyDTO(p.getId(), p.getActionType(),
                p.getRequesterRole().getId(), p.getRequesterRole().getName(),
                p.getWorkflow().getId(), p.getWorkflow().getName());
    }


    private Organization currentOrg() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found"));
        if (user.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't belong to an organization");
        }
        return user.getOrganization();
    }

}
