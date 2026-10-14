package com.infosys.procurement_system.common;

import com.infosys.procurement_system.common.exception.ForbiddenOperationException;
import com.infosys.procurement_system.user.User;
import java.util.Optional;

/**
 * Answers "who is the user making this request?".
 * Today the answer comes from a temporary X-User-Id header; in Milestone 4 a JWT-based
 * implementation replaces it and no service code changes.
 */
public interface CurrentUserProvider {

    Optional<User> findCurrentUser();

    default User requireCurrentUser() {
        return findCurrentUser().orElseThrow(() -> new ForbiddenOperationException(
                "No current user. Send the X-User-Id header (temporary until JWT login)."));
    }
}
