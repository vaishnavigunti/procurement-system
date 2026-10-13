package com.infosys.procurement_system.common.exception;

/** Thrown when the current user may not perform an action (for example approving their own request). Becomes HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
