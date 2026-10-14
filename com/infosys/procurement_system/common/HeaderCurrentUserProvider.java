package com.infosys.procurement_system.common;

import com.infosys.procurement_system.user.User;
import com.infosys.procurement_system.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Temporary implementation: reads the acting user's id from the X-User-Id request header. */
@Component
@RequiredArgsConstructor
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER = "X-User-Id";

    private final UserRepository userRepository;

    @Override
    public Optional<User> findCurrentUser() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        String value = attributes.getRequest().getHeader(HEADER);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return userRepository.findById(Long.parseLong(value.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
