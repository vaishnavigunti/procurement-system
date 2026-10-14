package com.infosys.procurement_system.audit;

/** Published after every audited change. Listeners (notifications, webhooks) can be added later without touching services. */
public record EntityChangedEvent(String entityType, Long entityId, AuditAction action, Long performedBy, String details) {
}
