package com.infosys.procurement_system.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Title and description shown on the Swagger page. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI procurementOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Enterprise Procurement System API")
                .version("v1")
                .description("Procurement master data, requisition workflows, purchase orders, analytics and audit."));
    }
}
