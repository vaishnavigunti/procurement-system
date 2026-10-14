package com.infosys.procurement_system.department;

import com.infosys.procurement_system.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "head_user_id")
    private Long headUserId;

    @Column(name = "annual_budget", nullable = false, precision = 15, scale = 2)
    private BigDecimal annualBudget;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
