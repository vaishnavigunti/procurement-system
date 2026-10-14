package com.infosys.procurement_system.supplier;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record SupplierRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 150) String contactPerson,
        @Email @Size(max = 150) String email,
        @Size(max = 30) String phone,
        String address,
        @NotBlank @Size(max = 50) String taxId,
        @NotEmpty Set<Long> categoryIds) {
}
