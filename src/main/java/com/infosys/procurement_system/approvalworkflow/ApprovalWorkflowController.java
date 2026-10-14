package com.infosys.procurement_system.approvalworkflow;

import com.infosys.procurement_system.common.ActiveStatusRequest;
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
@RequestMapping("/api/v1/approval-workflows")
@RequiredArgsConstructor
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService workflowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalWorkflowResponse create(@Valid @RequestBody ApprovalWorkflowRequest request) {
        return workflowService.create(request);
    }

    @GetMapping
    public PageResponse<ApprovalWorkflowResponse> list(@RequestParam(required = false) Boolean active,
                                                       @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return workflowService.list(active, pageable);
    }

    @GetMapping("/{id}")
    public ApprovalWorkflowResponse get(@PathVariable Long id) {
        return workflowService.get(id);
    }

    @PutMapping("/{id}")
    public ApprovalWorkflowResponse update(@PathVariable Long id, @Valid @RequestBody ApprovalWorkflowRequest request) {
        return workflowService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ApprovalWorkflowResponse setStatus(@PathVariable Long id, @Valid @RequestBody ActiveStatusRequest request) {
        return workflowService.setActive(id, request.active());
    }
}
