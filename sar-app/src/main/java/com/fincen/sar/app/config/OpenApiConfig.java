package com.fincen.sar.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sarSolutionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinCEN SAR Solution API")
                        .version("1.0.0")
                        .description("""
                                **FinCEN Suspicious Activity Report (SAR) Filing System**

                                A Spring Boot Modular Monolith for end-to-end SAR management.

                                ### Features
                                - SAR form creation and lifecycle management (DRAFT → IN_REVIEW → APPROVED → SUBMITTED → ACKNOWLEDGED)
                                - AML case management system integration for auto-population of subjects, accounts, and transactions
                                - Full audit history (Hibernate Envers + business event log)
                                - FinCEN BSA XML batch generation per [BSA XML 2.0 schema](https://www.fincen.gov/base/BSA_XML_2.0.xsd)
                                - XSD validation against [FinCENReferenceCodes.xsd](https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd)
                                - Automated eFiling with FinCEN BSA E-Filing system
                                - FinCEN acknowledgment processing and BSA ID tracking

                                ### FinCEN XSD Schemas
                                - Batch schema: `https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd`
                                - Base schema: `https://www.fincen.gov/base/BSA_XML_2.0.xsd`
                                - Reference codes: `https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd`
                                """)
                        .contact(new Contact()
                                .name("SAR Solution Support")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://example.com")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("Use Basic Auth. Default credentials: admin/admin123")));
    }
}
