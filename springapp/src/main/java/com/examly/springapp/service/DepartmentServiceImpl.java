package com.examly.springapp.service;

import com.examly.springapp.model.Department;
import com.examly.springapp.model.DepartmentRequest;
import com.examly.springapp.model.EmployeeProfile;
import com.examly.springapp.model.Organization;
import com.examly.springapp.model.OrganizationMembership;
import com.examly.springapp.model.TenantContext;
import com.examly.springapp.repository.DepartmentRepository;
import com.examly.springapp.repository.EmployeeProfileRepository;
import com.examly.springapp.repository.OrganizationMembershipRepository;
import com.examly.springapp.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private TenantContextService tenantContextService;

    @Override
    public Department createDepartment(DepartmentRequest request, Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        if (!ctx.hasAdminOrHrPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Only organization administrators and HR administrators can create departments.");
        }

        if (request.getName() == null || request.getName().trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department name is required.");
        }

        String deptName = request.getName().trim();
        if (departmentRepository.existsByOrganizationIdAndNameIgnoreCase(ctx.getOrganizationId(), deptName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Department with name '" + deptName + "' already exists in this organization.");
        }

        Organization org = organizationRepository.findById(ctx.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Organization record not found."));

        OrganizationMembership managerMembership = null;
        if (request.getManagerMembershipId() != null) {
            managerMembership = membershipRepository.findById(request.getManagerMembershipId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Manager membership ID #" + request.getManagerMembershipId() + " not found."));

            if (managerMembership.getOrganization() == null || !managerMembership.getOrganization().getId().equals(ctx.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cross-organization validation failed: Manager does not belong to this organization.");
            }

            if (!"ACTIVE".equalsIgnoreCase(managerMembership.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid manager membership: Membership status is inactive.");
            }
        }

        Department department = new Department(org, deptName, request.getCode(), managerMembership);
        return departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Department> getDepartments(Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        return departmentRepository.findByOrganizationId(ctx.getOrganizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id, Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        return departmentRepository.findByIdAndOrganizationId(id, ctx.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department #" + id + " not found in organization."));
    }

    @Override
    public Department updateDepartment(Long id, DepartmentRequest request, Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        if (!ctx.hasAdminOrHrPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Only organization administrators and HR administrators can update departments.");
        }

        Department department = departmentRepository.findByIdAndOrganizationId(id, ctx.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department #" + id + " not found in organization."));

        if (request.getName() != null && !request.getName().trim().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(department.getName()) &&
                    departmentRepository.existsByOrganizationIdAndNameIgnoreCase(ctx.getOrganizationId(), newName)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Department with name '" + newName + "' already exists in this organization.");
            }
            department.setName(newName);
        }

        if (request.getCode() != null) {
            department.setCode(request.getCode().trim());
        }

        if (request.getManagerMembershipId() != null) {
            OrganizationMembership managerMembership = membershipRepository.findById(request.getManagerMembershipId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Manager membership ID #" + request.getManagerMembershipId() + " not found."));

            if (managerMembership.getOrganization() == null || !managerMembership.getOrganization().getId().equals(ctx.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cross-organization validation failed: Manager does not belong to this organization.");
            }

            department.setManagerMembership(managerMembership);
        }

        return departmentRepository.save(department);
    }

    @Override
    public void assignEmployeeToDepartment(Long departmentId, String employeeId, Authentication authentication) {
        TenantContext ctx = tenantContextService.requireTenantContext(authentication);
        if (!ctx.hasAdminOrHrPrivileges()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access denied: Only organization administrators and HR administrators can assign employees to departments.");
        }

        Department department = departmentRepository.findByIdAndOrganizationId(departmentId, ctx.getOrganizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department #" + departmentId + " not found in organization."));

        EmployeeProfile profile = employeeProfileRepository.findByOrganizationIdAndEmployeeId(ctx.getOrganizationId(), employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee profile '" + employeeId + "' not found in organization."));

        if (profile.getOrganization() == null || !profile.getOrganization().getId().equals(ctx.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cross-organization violation: Employee profile does not belong to this organization.");
        }

        profile.setDepartmentRef(department);
        employeeProfileRepository.save(profile);
    }
}
