package com.infosys.procurement_system.common.exception;

/** Thrown for duplicates, invalid status transitions and broken business rules. Becomes HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
