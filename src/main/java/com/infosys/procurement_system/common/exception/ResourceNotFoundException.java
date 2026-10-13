package com.infosys.procurement_system.common.exception;

/** Thrown when a record does not exist. Becomes HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, Object id) {
        return new ResourceNotFoundException(entity + " not found with id " + id);
    }
}
