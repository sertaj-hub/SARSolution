package com.fincen.sar.caseintegration.client;

import com.fincen.sar.caseintegration.dto.CaseDto;

import java.util.List;
import java.util.Optional;

/**
 * Contract for AML Case Management System integration.
 * Implementations support REST, SOAP, or database direct access
 * depending on the case management system being integrated.
 */
public interface CaseManagementClient {

    /**
     * Fetch a case by its case ID from the AML system.
     */
    Optional<CaseDto> getCaseById(String caseId);

    /**
     * Search cases by various criteria.
     */
    List<CaseDto> searchCases(CaseSearchCriteria criteria);

    /**
     * Get the name of the case management system this client connects to.
     */
    String getSystemName();

    /**
     * Test connectivity to the case management system.
     */
    boolean isAvailable();
}
