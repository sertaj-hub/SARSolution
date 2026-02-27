package com.fincen.sar.efiling.service;

import com.fincen.sar.core.domain.EFilingBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * FinCEN BSA E-Filing submission service.
 * Handles HTTP submission of XML batch files to FinCEN BSA E-Filing portal.
 *
 * FinCEN BSA E-Filing API endpoint:
 * - Production: https://bsaefiling.fincen.gov/main.html (HTTPS upload)
 * - Test/Sandbox: https://bsaefiling-sandbox.fincen.gov
 *
 * Supports both REST API submission and file-based submission
 * (FinCEN provides multiple submission methods).
 */
@Slf4j
@Service
public class FinCenSubmissionService {

    private final RestTemplate restTemplate;

    public FinCenSubmissionService(
            @org.springframework.beans.factory.annotation.Qualifier("fincenRestTemplate")
            RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${sar.efiling.fincen.submission-url:https://bsaefiling-sandbox.fincen.gov/api/v1/batch}")
    private String submissionUrl;

    @Value("${sar.efiling.fincen.api-key:}")
    private String apiKey;

    @Value("${sar.efiling.fincen.institution-id:}")
    private String institutionId;

    @Value("${sar.efiling.mode:sandbox}")
    private String mode;

    /**
     * Submit a batch XML file to FinCEN BSA E-Filing.
     */
    public SubmissionResult submit(EFilingBatch batch) {
        if ("offline".equals(mode)) {
            log.info("Offline mode: simulating FinCEN submission for batch {}", batch.getBatchNumber());
            return SubmissionResult.success("OFFLINE-" + batch.getBatchNumber());
        }

        try {
            // Read the generated XML file
            String xmlContent = Files.readString(Path.of(batch.getXmlFilePath()), StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("X-API-Key", apiKey);
            headers.set("X-Institution-ID", institutionId);
            headers.set("X-Batch-Number", batch.getBatchNumber());

            HttpEntity<String> request = new HttpEntity<>(xmlContent, headers);

            ResponseEntity<FinCenApiResponse> response = restTemplate.exchange(
                    submissionUrl, HttpMethod.POST, request, FinCenApiResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FinCenApiResponse body = response.getBody();
                return SubmissionResult.success(body.getTrackingId());
            } else {
                return SubmissionResult.failure("HTTP " + response.getStatusCode() + " response");
            }
        } catch (Exception e) {
            log.error("FinCEN submission failed for batch {}: {}", batch.getBatchNumber(), e.getMessage());
            return SubmissionResult.failure(e.getMessage());
        }
    }

    /**
     * Poll FinCEN for acknowledgment of a submitted batch.
     */
    public AcknowledgmentPollResult pollAcknowledgment(String trackingId) {
        if ("offline".equals(mode)) {
            // Simulate accepted acknowledgment in offline mode
            return AcknowledgmentPollResult.available(buildSimulatedAcknowledgment(trackingId));
        }

        try {
            String url = submissionUrl + "/acknowledgment/" + trackingId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return AcknowledgmentPollResult.available(response.getBody());
            } else if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                return AcknowledgmentPollResult.notYetAvailable();
            } else {
                return AcknowledgmentPollResult.notYetAvailable();
            }
        } catch (Exception e) {
            log.warn("Failed to poll acknowledgment for tracking ID {}: {}", trackingId, e.getMessage());
            return AcknowledgmentPollResult.notYetAvailable();
        }
    }

    private String buildSimulatedAcknowledgment(String trackingId) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <EFilingBatchStatusXML StatusCode="A">
                    <EFilingSubmissionStatus>
                        <EFilingSubmissionStatusCode>A</EFilingSubmissionStatusCode>
                        <EFilingSubmissionStatusDescription>Accepted</EFilingSubmissionStatusDescription>
                    </EFilingSubmissionStatus>
                    <TrackingID>%s</TrackingID>
                </EFilingBatchStatusXML>
                """.formatted(trackingId);
    }

    // --- Result classes ---

    public static class SubmissionResult {
        private final boolean success;
        private final String trackingId;
        private final String errorMessage;

        private SubmissionResult(boolean success, String trackingId, String errorMessage) {
            this.success = success;
            this.trackingId = trackingId;
            this.errorMessage = errorMessage;
        }

        public static SubmissionResult success(String trackingId) {
            return new SubmissionResult(true, trackingId, null);
        }

        public static SubmissionResult failure(String error) {
            return new SubmissionResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getTrackingId() { return trackingId; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class AcknowledgmentPollResult {
        private final boolean available;
        private final String acknowledgmentXml;

        private AcknowledgmentPollResult(boolean available, String xml) {
            this.available = available;
            this.acknowledgmentXml = xml;
        }

        public static AcknowledgmentPollResult available(String xml) {
            return new AcknowledgmentPollResult(true, xml);
        }

        public static AcknowledgmentPollResult notYetAvailable() {
            return new AcknowledgmentPollResult(false, null);
        }

        public boolean isAvailable() { return available; }
        public String getAcknowledgmentXml() { return acknowledgmentXml; }
    }

    public static class FinCenApiResponse {
        private String trackingId;
        private String status;
        private String message;

        public String getTrackingId() { return trackingId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
