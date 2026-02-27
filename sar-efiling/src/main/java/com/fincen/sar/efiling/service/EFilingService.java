package com.fincen.sar.efiling.service;

import com.fincen.sar.audit.service.AuditService;
import com.fincen.sar.core.domain.EFilingBatch;
import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.enums.FilingStatus;
import com.fincen.sar.core.enums.SarStatus;
import com.fincen.sar.core.exception.SarException;
import com.fincen.sar.core.repository.EFilingBatchRepository;
import com.fincen.sar.core.repository.SarReportRepository;
import com.fincen.sar.core.util.ReportNumberGenerator;
import com.fincen.sar.efiling.xml.FinCenXmlGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FinCEN BSA E-Filing service.
 * Manages the complete eFiling workflow:
 * 1. Collect approved SAR reports
 * 2. Generate BSA XML batch file
 * 3. Submit to FinCEN BSA E-Filing system
 * 4. Process acknowledgment from FinCEN
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EFilingService {

    private final SarReportRepository sarReportRepository;
    private final EFilingBatchRepository eFilingBatchRepository;
    private final FinCenXmlGenerator xmlGenerator;
    private final FinCenSubmissionService submissionService;
    private final AuditService auditService;
    private final ReportNumberGenerator reportNumberGenerator;

    @Value("${sar.efiling.batch.output-dir:/tmp/sar-efiling}")
    private String batchOutputDir;

    @Value("${sar.efiling.batch.max-reports-per-batch:100}")
    private int maxReportsPerBatch;

    /**
     * Create a new eFiling batch from all approved SAR reports.
     * Called by the scheduled batch job.
     */
    public EFilingBatch createAndSubmitBatch() {
        List<SarReport> approvedReports = sarReportRepository.findApprovedReportsNotInBatch();

        if (approvedReports.isEmpty()) {
            log.info("No approved SAR reports found for batch processing");
            return null;
        }

        // Limit batch size
        List<SarReport> batchReports = approvedReports.stream()
                .limit(maxReportsPerBatch)
                .toList();

        log.info("Creating eFiling batch with {} SAR reports", batchReports.size());

        EFilingBatch batch = createBatch(batchReports);
        generateBatchXml(batch, batchReports);
        submitBatch(batch);

        return batch;
    }

    /**
     * Create an eFiling batch entity.
     */
    private EFilingBatch createBatch(List<SarReport> reports) {
        EFilingBatch batch = new EFilingBatch();
        batch.setBatchNumber(reportNumberGenerator.generateBatchNumber());
        batch.setActivityCount(reports.size());
        batch.setStatus(FilingStatus.PENDING);
        batch.setGeneratedAt(LocalDateTime.now());

        List<UUID> reportIds = new ArrayList<>();
        for (SarReport report : reports) {
            reportIds.add(report.getId());
            report.setBatchId(batch.getId());
        }
        batch.setSarReportIds(reportIds);

        batch = eFilingBatchRepository.save(batch);

        // Update reports with batch reference
        for (SarReport report : reports) {
            report.setBatchId(batch.getId());
            report.setStatus(SarStatus.SUBMITTED);
            sarReportRepository.save(report);
        }

        log.info("Created eFiling batch: {}", batch.getBatchNumber());
        return batch;
    }

    /**
     * Generate and save the BSA XML batch file.
     */
    private void generateBatchXml(EFilingBatch batch, List<SarReport> reports) {
        try {
            String xml = xmlGenerator.generateBatchXml(batch, reports);

            Path outputDir = Paths.get(batchOutputDir);
            Files.createDirectories(outputDir);

            String fileName = batch.getBatchNumber() + ".xml";
            Path filePath = outputDir.resolve(fileName);
            Files.write(filePath, xml.getBytes(StandardCharsets.UTF_8));

            batch.setXmlFileName(fileName);
            batch.setXmlFilePath(filePath.toAbsolutePath().toString());
            batch.setStatus(FilingStatus.BATCH_GENERATED);
            eFilingBatchRepository.save(batch);

            log.info("Generated XML batch file: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to generate batch XML: {}", e.getMessage(), e);
            batch.setStatus(FilingStatus.PENDING);
            batch.setErrorDescription("XML generation failed: " + e.getMessage());
            eFilingBatchRepository.save(batch);
            throw new SarException("BATCH_XML_FAILED", "Failed to generate batch XML", e);
        }
    }

    /**
     * Submit batch to FinCEN BSA E-Filing system.
     */
    private void submitBatch(EFilingBatch batch) {
        try {
            batch.setSubmissionAttempt(batch.getSubmissionAttempt() + 1);
            batch.setSubmittedAt(LocalDateTime.now());

            FinCenSubmissionService.SubmissionResult result = submissionService.submit(batch);

            if (result.isSuccess()) {
                batch.setStatus(FilingStatus.SUBMITTED);
                batch.setFincenTrackingId(result.getTrackingId());
                log.info("Batch {} submitted successfully. FinCEN tracking ID: {}",
                        batch.getBatchNumber(), result.getTrackingId());

                // Update all reports
                for (UUID reportId : batch.getSarReportIds()) {
                    sarReportRepository.findById(reportId).ifPresent(report -> {
                        report.setSubmittedAt(batch.getSubmittedAt());
                        report.setFincenTrackingNumber(result.getTrackingId());
                        sarReportRepository.save(report);
                        auditService.logEFilingSubmission(reportId, batch.getId(), batch.getBatchNumber());
                    });
                }
            } else {
                batch.setStatus(FilingStatus.PENDING);
                batch.setErrorDescription("Submission failed: " + result.getErrorMessage());
                log.error("Batch {} submission failed: {}", batch.getBatchNumber(), result.getErrorMessage());
            }

            eFilingBatchRepository.save(batch);
        } catch (Exception e) {
            log.error("Failed to submit batch {}: {}", batch.getBatchNumber(), e.getMessage(), e);
            batch.setStatus(FilingStatus.PENDING);
            batch.setErrorDescription("Submission exception: " + e.getMessage());
            eFilingBatchRepository.save(batch);
        }
    }

    /**
     * Process FinCEN acknowledgment for a submitted batch.
     * Called when acknowledgment is received (via polling or callback).
     */
    public void processAcknowledgment(String trackingId, String acknowledgmentXml) {
        EFilingBatch batch = eFilingBatchRepository.findByFincenTrackingId(trackingId)
                .orElseThrow(() -> new SarException("BATCH_NOT_FOUND",
                        "No batch found with tracking ID: " + trackingId));

        log.info("Processing acknowledgment for batch {}, tracking ID: {}",
                batch.getBatchNumber(), trackingId);

        // Parse the acknowledgment
        FinCenAcknowledgmentParser.AcknowledgmentResult result =
                parseAcknowledgment(acknowledgmentXml, batch);

        batch.setAcknowledgedAt(LocalDateTime.now());
        batch.setAcknowledgmentStatus(result.getStatus());
        batch.setErrorCodes(result.getErrorCodes());
        batch.setErrorDescription(result.getErrorDescription());

        if ("A".equals(result.getStatus())) {
            // Accepted
            batch.setStatus(FilingStatus.ACCEPTED);
            processAcceptedBatch(batch, result);
            log.info("Batch {} ACCEPTED by FinCEN", batch.getBatchNumber());
        } else if ("R".equals(result.getStatus())) {
            // Rejected
            batch.setStatus(FilingStatus.REJECTED);
            processRejectedBatch(batch, result);
            log.warn("Batch {} REJECTED by FinCEN: {}", batch.getBatchNumber(), result.getErrorDescription());
        } else {
            // Partial acceptance
            batch.setStatus(FilingStatus.PARTIALLY_ACCEPTED);
            log.info("Batch {} PARTIALLY ACCEPTED by FinCEN", batch.getBatchNumber());
        }

        eFilingBatchRepository.save(batch);
    }

    private void processAcceptedBatch(EFilingBatch batch,
                                       FinCenAcknowledgmentParser.AcknowledgmentResult result) {
        for (int i = 0; i < batch.getSarReportIds().size(); i++) {
            UUID reportId = batch.getSarReportIds().get(i);
            sarReportRepository.findById(reportId).ifPresent(report -> {
                report.setStatus(SarStatus.ACKNOWLEDGED);
                report.setAcknowledgedAt(batch.getAcknowledgedAt());

                // Assign BSA ID (from acknowledgment) - each activity gets unique BSA ID
                String bsaId = result.getBsaIdForReport(report.getReportNumber());
                if (bsaId != null) {
                    report.setBsaIdentifier(bsaId);
                }
                sarReportRepository.save(report);

                auditService.logFinCenAcknowledgment(reportId, bsaId, "ACCEPTED");
            });
        }
    }

    private void processRejectedBatch(EFilingBatch batch,
                                       FinCenAcknowledgmentParser.AcknowledgmentResult result) {
        for (UUID reportId : batch.getSarReportIds()) {
            sarReportRepository.findById(reportId).ifPresent(report -> {
                report.setStatus(SarStatus.REJECTED);
                report.setRejectionReason(result.getErrorDescription());
                sarReportRepository.save(report);

                auditService.logFinCenAcknowledgment(reportId, null, "REJECTED");
            });
        }
    }

    private FinCenAcknowledgmentParser.AcknowledgmentResult parseAcknowledgment(
            String xml, EFilingBatch batch) {
        return FinCenAcknowledgmentParser.parse(xml);
    }

    /**
     * Poll FinCEN for acknowledgment status of submitted batches.
     */
    public void pollForAcknowledgments() {
        List<EFilingBatch> submittedBatches = eFilingBatchRepository.findByStatus(FilingStatus.SUBMITTED);

        for (EFilingBatch batch : submittedBatches) {
            try {
                FinCenSubmissionService.AcknowledgmentPollResult pollResult =
                        submissionService.pollAcknowledgment(batch.getFincenTrackingId());

                if (pollResult.isAvailable()) {
                    processAcknowledgment(batch.getFincenTrackingId(), pollResult.getAcknowledgmentXml());
                }
            } catch (Exception e) {
                log.error("Failed to poll acknowledgment for batch {}: {}",
                        batch.getBatchNumber(), e.getMessage());
            }
        }
    }

    /**
     * Manually trigger resubmission of a rejected batch.
     */
    public EFilingBatch resubmitBatch(UUID batchId) {
        EFilingBatch batch = eFilingBatchRepository.findById(batchId)
                .orElseThrow(() -> new SarException("BATCH_NOT_FOUND", "Batch not found: " + batchId));

        if (batch.getStatus() != FilingStatus.REJECTED && batch.getStatus() != FilingStatus.PENDING) {
            throw new SarException("INVALID_BATCH_STATUS",
                    "Only REJECTED or PENDING batches can be resubmitted");
        }

        List<SarReport> reports = sarReportRepository.findByBatchId(batchId);
        submitBatch(batch);
        return batch;
    }
}
