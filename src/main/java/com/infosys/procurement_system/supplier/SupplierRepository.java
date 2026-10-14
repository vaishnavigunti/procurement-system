package com.infosys.procurement_system.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    boolean existsByTaxIdIgnoreCase(String taxId);

    boolean existsByTaxIdIgnoreCaseAndIdNot(String taxId, Long id);

    /** Next number from the database sequence (safe when several users register suppliers at once). */
    @Query(value = "select nextval('supplier_code_seq')", nativeQuery = true)
    Long nextSupplierNumber();
}
