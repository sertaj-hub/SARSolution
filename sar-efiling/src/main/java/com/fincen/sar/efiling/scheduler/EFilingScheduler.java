package com.fincen.sar.efiling.scheduler;

import com.fincen.sar.efiling.service.EFilingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Scheduled triggers for FinCEN SAR eFiling batch processes.
 *
 * Schedule:
 * - Batch submission: configurable (default: daily at 2:00 AM)
 * - Acknowledgment polling: every 30 minutes for submitted batches
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class EFilingScheduler {

    private final JobLauncher jobLauncher;
    private final Job eFilingJob;
    private final EFilingService eFilingService;

    @Value("${sar.efiling.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * Run the eFiling batch job on a configurable schedule.
     * Default: daily at 2:00 AM.
     * Override via: sar.efiling.scheduler.cron
     */
    @Scheduled(cron = "${sar.efiling.scheduler.cron:0 0 2 * * ?}")
    public void runEFilingBatch() {
        if (!schedulerEnabled) {
            log.debug("eFiling scheduler disabled");
            return;
        }

        log.info("Starting scheduled FinCEN SAR eFiling batch job");
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("runTime", new Date())
                    .addString("trigger", "SCHEDULED")
                    .toJobParameters();

            jobLauncher.run(eFilingJob, params);
        } catch (Exception e) {
            log.error("Failed to launch eFiling batch job: {}", e.getMessage(), e);
        }
    }

    /**
     * Poll FinCEN for acknowledgments every 30 minutes.
     * Checks submitted batches awaiting FinCEN response.
     */
    @Scheduled(fixedDelayString = "${sar.efiling.scheduler.poll-interval-ms:1800000}")
    public void pollAcknowledgments() {
        if (!schedulerEnabled) return;
        try {
            log.debug("Polling FinCEN for batch acknowledgments");
            eFilingService.pollForAcknowledgments();
        } catch (Exception e) {
            log.error("Acknowledgment polling error: {}", e.getMessage(), e);
        }
    }
}
