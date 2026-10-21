package com.exercise.contactBook.service;

import com.exercise.contactBook.dto.PermissionDTO;
import com.exercise.contactBook.dto.RoleDTO;
import com.exercise.contactBook.dto.RoleRequestDTO;
import com.exercise.contactBook.enums.Permission;
import com.exercise.contactBook.enums.Role;
import com.exercise.contactBook.exception.ResourceNotFoundException;
import com.exercise.contactBook.model.OrgRole;
import com.exercise.contactBook.model.Organization;
import com.exercise.contactBook.model.User;
import com.exercise.contactBook.repository.ApprovalPolicyRepository;
import com.exercise.contactBook.repository.ApprovalStepRepository;
import com.exercise.contactBook.repository.OrgRoleRepository;
import com.exercise.contactBook.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class RoleService {
    private final OrgRoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalPolicyRepository approvalPolicyRepository;
    private static final Set<String> RESERVED_NAMES = Set.of("SUPER_ADMIN", "USER");

    public RoleService(OrgRoleRepository roleRepository, UserRepository userRepository, ApprovalStepRepository approvalStepRepository, ApprovalPolicyRepository approvalPolicyRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.approvalStepRepository = approvalStepRepository;
        this.approvalPolicyRepository = approvalPolicyRepository;
    }

    private static final Map<String, Set<Permission>> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("ADMIN", EnumSet.allOf(Permission.class));
        DEFAULTS.put("MANAGER", EnumSet.of(Permission.CONTACT_COMMON_DIRECT, Permission.REQUEST_APPROVE));
        DEFAULTS.put("EMPLOYEE", EnumSet.noneOf(Permission.class));
    }

    @Transactional
    public Map<String, OrgRole> ensureDefaultRoles(Organization org) {
        Map<String, OrgRole> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Permission>> e : DEFAULTS.entrySet()) {
            OrgRole role = roleRepository.findByOrganizationAndNameIgnoreCase(org, e.getKey())
                    .orElseGet(() -> {
                        OrgRole r = new OrgRole();
                        r.setOrganization(org);
                        r.setName(e.getKey());
                        r.setSystemRole(true);
                        r.setPermissions(new HashSet<>(e.getValue()));
                        return roleRepository.save(r);
                    });
            result.put(e.getKey(), role);
        }
        return result;
    }


    @Transactional
    public OrgRole forLegacyRole(Organization org, Role legacy) {
        if (org == null || legacy == null) return null;
        return ensureDefaultRoles(org).get(legacy.name());
    }

    public List<RoleDTO> listRoles(){
        Organization org = currentOrg();
        return roleRepository.findByOrganization(org).stream()
                .sorted(Comparator.comparing(OrgRole::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto)
                .toList();
    }

    public List<PermissionDTO> listPermissions(){
        return Arrays.stream(Permission.values())
                .map(p -> new PermissionDTO(p.name(), p.getDescription()))
                .toList();
    }

    @Transactional
    public RoleDTO createRole(RoleRequestDTO dto){
        Organization org = currentOrg();
        String name = dto.getName().trim();
        assertNotReserved(name);
        if(roleRepository.findByOrganizationAndNameIgnoreCase(org , name).isPresent()){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role with this name is already exists");
        }
        OrgRole role = new OrgRole();
        role.setOrganization(org);
        role.setName(name);
        role.setSystemRole(false);
        role.setPermissions(new HashSet<>(dto.getPermissions()));
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public RoleDTO updateRole(Long id, RoleRequestDTO dto){
        Organization org = currentOrg();
        OrgRole role = roleRepository.findByIdAndOrganization(id,org)
                .orElseThrow(() -> new ResourceNotFoundException("Role Not found"));
        if(role.getName().equalsIgnoreCase("ADMIN")){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The Admin role cant be changed");
        }
        String name = dto.getName().trim();

        assertNotReserved(name);

        if(role.isSystemRole() && !role.getName().equalsIgnoreCase(name)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built in roles cant be renamed");
        }
        roleRepository.findByOrganizationAndNameIgnoreCase(org,name)
                .filter(other ->!other.getId().equals(id))
                .ifPresent(other ->{
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A role with this name already exists");
                });
        boolean losesReview = role.getPermissions().contains(Permission.REQUEST_APPROVE)
                && !dto.getPermissions().contains(Permission.REQUEST_APPROVE);
        if(losesReview && approvalStepRepository.existsByApproverRole(role)){
            throw new ResponseStatusException(HttpStatus.CONFLICT , "This role is an approver in a workflow remove it from the workflow first");
        }
        role.setName(name);
        role.getPermissions().clear();
        role.getPermissions().addAll(dto.getPermissions());
        return toDto(roleRepository.save(role));
    }
    @Transactional
    public void deleteRole(Long id){
        Organization org = currentOrg();
        OrgRole role = roleRepository.findByIdAndOrganization(id,org)
                .orElseThrow(() -> new ResourceNotFoundException("Role Not found"));
        if(role.isSystemRole() ){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built in roles cant be deleted");
        }
        long users = userRepository.countByOrgRole(role);
        if(users > 0){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    users + " people still have this role. Move them to another role first");
        }
        if(approvalStepRepository.existsByApproverRole(role) || approvalPolicyRepository.existsByRequesterRole(role)){
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    users + "This role is used in an approval workflow. Remove it from workflow first");
        }
        roleRepository.delete(role);
    }

    @Transactional
    public void syncAdminPermissions(Organization org){
        OrgRole admin =  ensureDefaultRoles(org).get("ADMIN");
        if(admin.getPermissions().addAll(EnumSet.allOf(Permission.class))){
            roleRepository.save(admin);
        }
    }

    //helperrrrr

    private RoleDTO toDto(OrgRole role){
        return new RoleDTO(role.getId(), role.getName(), role.isSystemRole(),
                role.getPermissions().stream().map(Enum::name).sorted().toList(),
                userRepository.countByOrgRole(role));
    }
    private void assertNotReserved(String name){
        if(RESERVED_NAMES.contains(name.toUpperCase())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This role name is reserved");
        }
    }
    private Organization currentOrg(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user not found"));
        if(user.getOrganization() == null){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You dont belong to an organization");
        }
        return user.getOrganization();
    }
 }

