package com.infosys.procurement_system.common.exception;

/** Thrown when a request is well-formed JSON but breaks a simple input rule. Becomes HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
