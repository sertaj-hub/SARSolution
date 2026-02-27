package com.fincen.sar.api.controller;

import com.fincen.sar.core.domain.EFilingBatch;
import com.fincen.sar.core.enums.FilingStatus;
import com.fincen.sar.core.repository.EFilingBatchRepository;
import com.fincen.sar.efiling.service.EFilingService;
import com.fincen.sar.efiling.xml.FinCenXmlGenerator;
import com.fincen.sar.efiling.xml.FinCenXsdValidator;
import com.fincen.sar.core.repository.SarReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for FinCEN BSA E-Filing operations.
 * Provides batch management, XML preview, and acknowledgment processing.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/efiling")
@RequiredArgsConstructor
@Tag(name = "eFiling", description = "FinCEN BSA E-Filing batch management")
public class EFilingController {

    private final EFilingService eFilingService;
    private final EFilingBatchRepository eFilingBatchRepository;
    private final SarReportRepository sarReportRepository;
    private final FinCenXmlGenerator xmlGenerator;
    private final FinCenXsdValidator xsdValidator;
    private final JobLauncher jobLauncher;
    private final Job eFilingJob;

    @PostMapping("/batches/trigger")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Manually trigger eFiling batch job",
            description = "Collects all APPROVED SAR reports, generates BSA XML, and submits to FinCEN")
    public ResponseEntity<Map<String, String>> triggerBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("runTime", new Date())
                    .addString("trigger", "MANUAL")
                    .toJobParameters();
            jobLauncher.run(eFilingJob, params);
            return ResponseEntity.ok(Map.of("status", "BATCH_JOB_LAUNCHED",
                    "message", "eFiling batch job started successfully"));
        } catch (Exception e) {
            log.error("Failed to trigger eFiling batch: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "List all eFiling batches")
    public ResponseEntity<List<EFilingBatch>> listBatches(
            @RequestParam(required = false) FilingStatus status) {
        List<EFilingBatch> batches = status != null
                ? eFilingBatchRepository.findByStatus(status)
                : eFilingBatchRepository.findAll();
        return ResponseEntity.ok(batches);
    }

    @GetMapping("/batches/{batchId}")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN', 'VIEWER')")
    @Operation(summary = "Get eFiling batch by ID")
    public ResponseEntity<EFilingBatch> getBatch(@PathVariable UUID batchId) {
        return eFilingBatchRepository.findById(batchId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batches/{batchId}/resubmit")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Resubmit a rejected or failed batch to FinCEN")
    public ResponseEntity<EFilingBatch> resubmitBatch(@PathVariable UUID batchId) {
        return ResponseEntity.ok(eFilingService.resubmitBatch(batchId));
    }

    @PostMapping("/batches/{batchId}/process-acknowledgment")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Process FinCEN acknowledgment XML for a batch",
            description = "Manually process a FinCEN acknowledgment response and update SAR statuses")
    public ResponseEntity<Map<String, String>> processAcknowledgment(
            @PathVariable UUID batchId,
            @RequestBody String acknowledgmentXml) {
        EFilingBatch batch = eFilingBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found: " + batchId));
        eFilingService.processAcknowledgment(batch.getFincenTrackingId(), acknowledgmentXml);
        return ResponseEntity.ok(Map.of("status", "ACKNOWLEDGED",
                "message", "Acknowledgment processed successfully"));
    }

    @PostMapping("/batches/poll-acknowledgments")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Poll FinCEN for pending batch acknowledgments")
    public ResponseEntity<Map<String, String>> pollAcknowledgments() {
        eFilingService.pollForAcknowledgments();
        return ResponseEntity.ok(Map.of("status", "POLLED",
                "message", "Acknowledgment polling completed"));
    }

    @GetMapping("/reports/{sarId}/preview-xml")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Preview the BSA XML that would be generated for a SAR report",
            description = "Generates and returns the XML without submitting to FinCEN")
    public ResponseEntity<String> previewXml(@PathVariable UUID sarId) {
        return sarReportRepository.findById(sarId)
                .map(report -> {
                    try {
                        EFilingBatch mockBatch = new EFilingBatch();
                        mockBatch.setBatchNumber("PREVIEW");
                        String xml = xmlGenerator.generateBatchXml(mockBatch, List.of(report));
                        return ResponseEntity.ok()
                                .header("Content-Type", "application/xml")
                                .body(xml);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError()
                                .<String>body("XML generation failed: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reports/{sarId}/validate-xml")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER', 'ADMIN')")
    @Operation(summary = "Validate SAR report XML against FinCEN XSD schemas",
            description = "Validates against EFL_SARXBatchSchema.xsd, BSA_XML_2.0.xsd, and FinCENReferenceCodes.xsd")
    public ResponseEntity<Map<String, Object>> validateXml(@PathVariable UUID sarId) {
        return sarReportRepository.findById(sarId)
                .map(report -> {
                    try {
                        EFilingBatch mockBatch = new EFilingBatch();
                        mockBatch.setBatchNumber("VALIDATE");
                        String xml = xmlGenerator.generateBatchXml(mockBatch, List.of(report));

                        FinCenXsdValidator.ValidationResult result = xsdValidator.validate(xml);
                        Map<String, Object> response = new java.util.HashMap<>();
                        response.put("valid", result.isPassed());
                        response.put("skipped", result.isSkipped());
                        response.put("skipReason", result.getSkipReason());
                        response.put("errors", result.getErrors());
                        response.put("warnings", result.getWarnings());
                        response.put("schemas", Map.of(
                                "batchSchema", "https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd",
                                "baseSchema", "https://www.fincen.gov/base/BSA_XML_2.0.xsd",
                                "referenceCodes", FinCenXsdValidator.FINCEN_REFERENCE_CODES_XSD_URL
                        ));
                        return ResponseEntity.ok(response);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError()
                                .<Map<String, Object>>body(Map.of("error", e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
