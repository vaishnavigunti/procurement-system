package com.infosys.procurement_system.category;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private static final String TYPE = "CATEGORY";

    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public PageResponse<CategoryResponse> list(Boolean active, Pageable pageable) {
        Page<Category> page = active == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findByActive(active, pageable);
        return PageResponse.from(page, CategoryResponse::from);
    }

    public CategoryResponse get(Long id) {
        return CategoryResponse.from(find(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A category with this code already exists.");
        }
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with this name already exists.");
        }
        Category category = new Category();
        apply(category, code, name, request);
        Category saved = categoryRepository.save(category);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED, "Created category " + saved.getCode());
        return CategoryResponse.from(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();
        if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("A category with this code already exists.");
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("A category with this name already exists.");
        }
        apply(category, code, name, request);
        auditService.record(TYPE, id, AuditAction.UPDATED, "Updated category " + code);
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse setActive(Long id, boolean active) {
        Category category = find(id);
        category.setActive(active);
        auditService.record(TYPE, id, AuditAction.STATUS_CHANGED, active ? "Activated" : "Deactivated");
        return CategoryResponse.from(category);
    }

    private void apply(Category category, String code, String name, CategoryRequest request) {
        category.setCode(code);
        category.setName(name);
        category.setDescription(request.description());
    }

    private Category find(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
