package com.infosys.procurement_system.supplier;

import com.infosys.procurement_system.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierResponse register(@Valid @RequestBody SupplierRequest request) {
        return supplierService.create(request);
    }

    @GetMapping
    public PageResponse<SupplierResponse> list(@RequestParam(required = false) SupplierStatus status,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false) String q,
                                               @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return supplierService.list(status, categoryId, q, pageable);
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse changeStatus(@PathVariable Long id, @Valid @RequestBody SupplierStatusRequest request) {
        return supplierService.changeStatus(id, request);
    }

    @PatchMapping("/{id}/evaluation")
    public SupplierResponse evaluate(@PathVariable Long id, @Valid @RequestBody SupplierEvaluationRequest request) {
        return supplierService.evaluate(id, request);
    }
}
