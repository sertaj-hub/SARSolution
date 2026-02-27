package com.fincen.sar.caseintegration.client;

import com.fincen.sar.caseintegration.dto.CaseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * REST-based AML Case Management System client.
 * Configurable for different case management systems (NICE Actimize, Oracle FCCM, etc.)
 * Activated when case-management.type=rest
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sar.case-management.type", havingValue = "rest", matchIfMissing = true)
public class RestCaseManagementClient implements CaseManagementClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String systemName;
    private final String apiKey;

    public RestCaseManagementClient(
            @org.springframework.beans.factory.annotation.Qualifier("caseManagementRestTemplate")
            RestTemplate restTemplate,
            @Value("${sar.case-management.base-url:http://localhost:8090}") String baseUrl,
            @Value("${sar.case-management.system-name:AML-CMS}") String systemName,
            @Value("${sar.case-management.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.systemName = systemName;
        this.apiKey = apiKey;
    }

    @Override
    public Optional<CaseDto> getCaseById(String caseId) {
        try {
            String url = baseUrl + "/api/v1/cases/{caseId}";
            log.debug("Fetching case {} from {}", caseId, systemName);
            ResponseEntity<CaseDto> response = restTemplate.getForEntity(url, CaseDto.class, caseId);
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.error("Failed to fetch case {} from {}: {}", caseId, systemName, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public List<CaseDto> searchCases(CaseSearchCriteria criteria) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v1/cases/search");
            if (criteria.getCaseNumber() != null) builder.queryParam("caseNumber", criteria.getCaseNumber());
            if (criteria.getAlertId() != null) builder.queryParam("alertId", criteria.getAlertId());
            if (criteria.getSubjectName() != null) builder.queryParam("subjectName", criteria.getSubjectName());
            if (criteria.getFromDate() != null) builder.queryParam("fromDate", criteria.getFromDate());
            if (criteria.getToDate() != null) builder.queryParam("toDate", criteria.getToDate());
            builder.queryParam("page", criteria.getPage());
            builder.queryParam("size", criteria.getSize() > 0 ? criteria.getSize() : 20);

            ResponseEntity<List<CaseDto>> response = restTemplate.exchange(
                    builder.toUriString(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CaseDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.error("Failed to search cases in {}: {}", systemName, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public String getSystemName() {
        return systemName;
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = baseUrl + "/api/v1/health";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.warn("Case management system {} is not available: {}", systemName, e.getMessage());
            return false;
        }
    }
}
