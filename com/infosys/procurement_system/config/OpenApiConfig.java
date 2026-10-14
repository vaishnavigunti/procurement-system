package com.infosys.procurement_system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger page title, plus an "Authorize" button where you enter the acting user's id (temporary). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI procurementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise Procurement System API")
                        .version("v1")
                        .description("Procurement master data, requisition workflows, purchase orders, analytics and audit."))
                .components(new Components().addSecuritySchemes("X-User-Id", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-User-Id")
                        .description("Temporary: id of the acting user. Replaced by JWT login in Milestone 4.")))
                .addSecurityItem(new SecurityRequirement().addList("X-User-Id"));
    }
}
