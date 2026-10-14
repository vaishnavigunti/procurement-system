package com.infosys.procurement_system.department;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentService {

    private static final String TYPE = "DEPARTMENT";

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PageResponse<DepartmentResponse> list(Boolean active, Pageable pageable) {
        Page<Department> page = active == null
                ? departmentRepository.findAll(pageable)
                : departmentRepository.findByActive(active, pageable);
        return PageResponse.from(page, DepartmentResponse::from);
    }

    public DepartmentResponse get(Long id) {
        return DepartmentResponse.from(find(id));
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        if (departmentRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A department with this code already exists.");
        }
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A department with this name already exists.");
        }
        checkHead(request.headUserId());
        Department department = new Department();
        apply(department, code, name, request);
        Department saved = departmentRepository.save(department);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED, "Created department " + saved.getCode());
        return DepartmentResponse.from(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = find(id);
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        if (departmentRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("A department with this code already exists.");
        }
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A department with this name already exists.");
        }
        checkHead(request.headUserId());
        apply(department, code, name, request);
        auditService.record(TYPE, id, AuditAction.UPDATED, "Updated department " + code);
        return DepartmentResponse.from(department);
    }

    @Transactional
    public DepartmentResponse setActive(Long id, boolean active) {
        Department department = find(id);
        if (!active && userRepository.countByDepartmentIdAndActiveTrue(id) > 0) {
            throw new ConflictException("Department still has active users. Move or deactivate them first.");
        }
        department.setActive(active);
        auditService.record(TYPE, id, AuditAction.STATUS_CHANGED, active ? "Activated" : "Deactivated");
        return DepartmentResponse.from(department);
    }

    private void apply(Department department, String code, String name, DepartmentRequest request) {
        department.setCode(code);
        department.setName(name);
        department.setHeadUserId(request.headUserId());
        department.setAnnualBudget(request.annualBudget());
    }

    private void checkHead(Long headUserId) {
        if (headUserId != null && !userRepository.existsById(headUserId)) {
            throw ResourceNotFoundException.of("User", headUserId);
        }
    }

    private Department find(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }
}
