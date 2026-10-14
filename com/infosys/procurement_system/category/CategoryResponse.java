package com.infosys.procurement_system.category;

public record CategoryResponse(Long id, String code, String name, String description, boolean active) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.isActive());
    }
}
