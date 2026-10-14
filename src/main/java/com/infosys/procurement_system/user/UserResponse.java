package com.infosys.procurement_system.user;

public record UserResponse(Long id, String employeeCode, String fullName, String email, String role,
                           Long departmentId, String departmentName, Long managerId, boolean active) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getEmployeeCode(), u.getFullName(), u.getEmail(),
                u.getRole().getName(),
                u.getDepartment().getId(), u.getDepartment().getName(),
                u.getManager() == null ? null : u.getManager().getId(),
                u.isActive());
    }
}
