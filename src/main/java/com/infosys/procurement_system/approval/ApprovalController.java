package com.infosys.procurement_system.approval;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /** The approver's inbox: requisitions waiting for the current user's decision. */
    @GetMapping("/pending")
    public List<ApprovalResponse> pending() {
        return approvalService.pendingForMe();
    }
}
