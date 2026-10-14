package com.infosys.procurement_system.user;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.department.Department;
import com.infosys.procurement_system.department.DepartmentRepository;
import com.infosys.procurement_system.role.Role;
import com.infosys.procurement_system.role.RoleRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final String TYPE = "USER";
    private static final String ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PageResponse<UserResponse> list(Long roleId, Long departmentId, Boolean active, String q,
                                           Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (roleId != null) {
                predicates.add(cb.equal(root.get("role").get("id"), roleId));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("employeeCode")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(userRepository.findAll(spec, pageable), UserResponse::from);
    }

    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase();
        String code = request.employeeCode().trim().toUpperCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with this email already exists.");
        }
        if (userRepository.existsByEmployeeCode(code)) {
            throw new ConflictException("A user with this employee code already exists.");
        }
        User user = new User();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        apply(user, code, request.fullName(), email, request.roleId(), request.departmentId(), request.managerId());
        User saved = userRepository.save(user);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED, "Created user " + saved.getEmployeeCode());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = find(id);
        String email = request.email().trim().toLowerCase();
        String code = request.employeeCode().trim().toUpperCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("A user with this email already exists.");
        }
        if (userRepository.existsByEmployeeCodeAndIdNot(code, id)) {
            throw new ConflictException("A user with this employee code already exists.");
        }
        Role newRole = findRole(request.roleId());
        // Checked before any change is made, so the count still reflects the database.
        if (user.isActive() && ADMIN.equals(user.getRole().getName()) && !ADMIN.equals(newRole.getName())
                && userRepository.countByRoleNameAndActiveTrue(ADMIN) <= 1) {
            throw new ConflictException("Cannot change the role of the last active admin.");
        }
        apply(user, code, request.fullName(), email, request.roleId(), request.departmentId(), request.managerId());
        auditService.record(TYPE, id, AuditAction.UPDATED, "Updated user " + code);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setActive(Long id, boolean active) {
        User user = find(id);
        if (!active && user.isActive() && ADMIN.equals(user.getRole().getName())
                && userRepository.countByRoleNameAndActiveTrue(ADMIN) <= 1) {
            throw new ConflictException("Cannot deactivate the last active admin.");
        }
        user.setActive(active);
        auditService.record(TYPE, id, AuditAction.STATUS_CHANGED, active ? "Activated" : "Deactivated");
        return UserResponse.from(user);
    }

    private void apply(User user, String code, String fullName, String email, Long roleId, Long departmentId,
                       Long managerId) {
        Role role = findRole(roleId);
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Department", departmentId));
        if (!department.isActive()) {
            throw new ConflictException("Department is not active.");
        }
        User manager = null;
        if (managerId != null) {
            if (managerId.equals(user.getId())) {
                throw new ConflictException("A user cannot be their own manager.");
            }
            manager = userRepository.findById(managerId)
                    .orElseThrow(() -> ResourceNotFoundException.of("User", managerId));
            if (!manager.isActive()) {
                throw new ConflictException("Manager is not active.");
            }
        }
        user.setEmployeeCode(code);
        user.setFullName(fullName.trim());
        user.setEmail(email);
        user.setRole(role);
        user.setDepartment(department);
        user.setManager(manager);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> ResourceNotFoundException.of("Role", roleId));
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
