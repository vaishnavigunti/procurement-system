package com.infosys.procurement_system.requisition;

import com.infosys.procurement_system.approval.ApprovalDecisionRequest;
import com.infosys.procurement_system.approval.ApprovalResponse;
import com.infosys.procurement_system.approval.ApprovalService;
import com.infosys.procurement_system.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/requisitions")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;
    private final ApprovalService approvalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequisitionResponse create(@Valid @RequestBody RequisitionRequest request) {
        return requisitionService.create(request);
    }

    @GetMapping
    public PageResponse<RequisitionResponse> list(
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(required = false) Boolean mine,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return requisitionService.list(status, mine, pageable);
    }

    @GetMapping("/{id}")
    public RequisitionResponse get(@PathVariable Long id) {
        return requisitionService.get(id);
    }

    @PutMapping("/{id}")
    public RequisitionResponse update(@PathVariable Long id, @Valid @RequestBody RequisitionRequest request) {
        return requisitionService.update(id, request);
    }

    @PostMapping("/{id}/submit")
    public RequisitionResponse submit(@PathVariable Long id, @Valid @RequestBody RequisitionSubmitRequest request) {
        return requisitionService.submit(id, request);
    }

    @PostMapping("/{id}/cancel")
    public RequisitionResponse cancel(@PathVariable Long id, @Valid @RequestBody RequisitionCancelRequest request) {
        return requisitionService.cancel(id, request);
    }

    @GetMapping("/{id}/approvals")
    public List<ApprovalResponse> approvals(@PathVariable Long id) {
        return requisitionService.approvalHistory(id);
    }

    @PostMapping("/{id}/approve")
    public RequisitionResponse approve(@PathVariable Long id, @Valid @RequestBody(required = false) ApprovalDecisionRequest request) {
        return approvalService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    public RequisitionResponse reject(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return approvalService.reject(id, request);
    }

    @PostMapping("/{id}/return")
    public RequisitionResponse returnToRequester(@PathVariable Long id, @Valid @RequestBody ApprovalDecisionRequest request) {
        return approvalService.returnToRequester(id, request);
    }
}
