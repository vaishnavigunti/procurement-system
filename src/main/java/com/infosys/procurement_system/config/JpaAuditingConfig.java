package com.infosys.procurement_system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Switches on automatic created_at / updated_at handling. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
