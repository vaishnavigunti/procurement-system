package com.infosys.procurement_system.department;

import java.math.BigDecimal;

public record DepartmentResponse(Long id, String code, String name, Long headUserId, BigDecimal annualBudget,
                                 boolean active) {

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName(), d.getHeadUserId(), d.getAnnualBudget(),
                d.isActive());
    }
}
