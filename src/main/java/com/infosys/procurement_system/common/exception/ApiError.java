package com.infosys.procurement_system.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** The JSON body returned for every error. */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       List<FieldIssue> fieldErrors) {

    public record FieldIssue(String field, String message) {
    }

    public static ApiError of(HttpStatus status, String message, String path, List<FieldIssue> fieldErrors) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
