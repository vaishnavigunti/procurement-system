package com.infosys.procurement_system.config;

import com.infosys.procurement_system.department.Department;
import com.infosys.procurement_system.department.DepartmentRepository;
import com.infosys.procurement_system.role.RoleRepository;
import com.infosys.procurement_system.user.User;
import com.infosys.procurement_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Development helper: when app.seed-sample-users=true and the users table is empty,
 * creates one sample user per role so the workflow can be tried straight away.
 * All sample users share the password Password@123.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleDataLoader implements ApplicationRunner {

    private static final String PASSWORD = "Password@123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-sample-users:false}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        Department it = departmentRepository.findByCode("IT").orElseThrow();
        Department fin = departmentRepository.findByCode("FIN").orElseThrow();

        User manager = save("M001", "Ravi Manager", "ravi.manager@procurement.test", "MANAGER", it, null);
        User head = save("D001", "Meena Head", "meena.head@procurement.test", "DEPARTMENT_HEAD", it, null);
        save("E001", "Asha Employee", "asha.employee@procurement.test", "EMPLOYEE", it, manager);
        User finance = save("F001", "Kiran Finance", "kiran.finance@procurement.test", "FINANCE_HEAD", fin, null);
        save("P001", "Suresh Procurement", "suresh.procurement@procurement.test", "PROCUREMENT_OFFICER", fin, null);
        save("A001", "Admin User", "admin@procurement.test", "ADMIN", it, null);

        it.setHeadUserId(head.getId());
        fin.setHeadUserId(finance.getId());
        log.info("Sample users created (password {}). Ids start at {}.", PASSWORD, manager.getId());
    }

    private User save(String code, String name, String email, String role, Department dept, User manager) {
        User user = new User();
        user.setEmployeeCode(code);
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(roleRepository.findByName(role).orElseThrow());
        user.setDepartment(dept);
        user.setManager(manager);
        return userRepository.save(user);
    }
}
