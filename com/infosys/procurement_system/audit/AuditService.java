package com.infosys.procurement_system.audit;

import com.infosys.procurement_system.common.CurrentUserProvider;
import com.infosys.procurement_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Every service calls record(...) after an important change: it writes audit_logs and publishes an event. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher events;

    @Transactional
    public void record(String entityType, Long entityId, AuditAction action, String details) {
        Long actor = currentUserProvider.findCurrentUser().map(User::getId).orElse(null);
        AuditLog entry = new AuditLog();
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setAction(action);
        entry.setPerformedBy(actor);
        entry.setDetails(details);
        entry.setIpAddress(clientIp());
        repository.save(entry);
        events.publishEvent(new EntityChangedEvent(entityType, entityId, action, actor, details));
    }

    private String clientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
