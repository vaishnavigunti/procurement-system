package com.infosys.procurement_system.approval;

import jakarta.validation.constraints.Size;

/** Comments are optional for approve, required for reject and return. */
public record ApprovalDecisionRequest(@Size(max = 2000) String comments) {
}
