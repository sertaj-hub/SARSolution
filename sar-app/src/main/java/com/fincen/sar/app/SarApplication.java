package com.fincen.sar.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * FinCEN SAR Solution - Spring Boot Modular Monolith
 *
 * Modules:
 * - sar-core:              Domain entities, repositories, shared utilities
 * - sar-case-integration:  AML case management system client
 * - sar-form:              SAR form lifecycle and validation service
 * - sar-audit:             Audit history (Envers + business events)
 * - sar-efiling:           FinCEN BSA E-Filing XML generation and submission
 * - sar-api:               REST API layer (controllers, OpenAPI docs)
 *
 * FinCEN XSD Schemas used:
 * - EFL_SARXBatchSchema.xsd: https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd
 * - BSA_XML_2.0.xsd:         https://www.fincen.gov/base/BSA_XML_2.0.xsd
 * - FinCENReferenceCodes.xsd: https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.fincen.sar.core",
        "com.fincen.sar.caseintegration",
        "com.fincen.sar.form",
        "com.fincen.sar.audit",
        "com.fincen.sar.efiling",
        "com.fincen.sar.api",
        "com.fincen.sar.app"
})
@EntityScan(basePackages = "com.fincen.sar.core.domain")
@EnableJpaRepositories(basePackages = "com.fincen.sar.core.repository")
public class SarApplication {

    public static void main(String[] args) {
        SpringApplication.run(SarApplication.class, args);
    }
}
