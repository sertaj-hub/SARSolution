package com.fincen.sar.efiling.batch;

import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.enums.SarStatus;
import com.fincen.sar.core.repository.SarReportRepository;
import com.fincen.sar.efiling.service.EFilingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

/**
 * Spring Batch configuration for FinCEN SAR eFiling batch job.
 *
 * Job flow:
 * Step 1: Validate approved SAR reports against FinCEN XSD schemas
 * Step 2: Generate BSA XML batch file
 * Step 3: Submit XML batch to FinCEN BSA E-Filing
 * Step 4: Poll and process FinCEN acknowledgments
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class EFilingBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SarReportRepository sarReportRepository;
    private final EFilingService eFilingService;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job eFilingJob() {
        return new JobBuilder("eFilingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(eFilingJobListener())
                .start(validateSarReportsStep())
                .next(submitBatchStep())
                .next(pollAcknowledgmentStep())
                .build();
    }

    /**
     * Step 1: Read and validate approved SAR reports.
     */
    @Bean
    public Step validateSarReportsStep() {
        return new StepBuilder("validateSarReportsStep", jobRepository)
                .<SarReport, SarReport>chunk(CHUNK_SIZE, transactionManager)
                .reader(approvedSarReportReader())
                .processor(sarValidationProcessor())
                .writer(sarValidationWriter())
                .faultTolerant()
                .skipLimit(5)
                .skip(Exception.class)
                .build();
    }

    /**
     * Step 2: Generate XML batch and submit to FinCEN.
     */
    @Bean
    public Step submitBatchStep() {
        return new StepBuilder("submitBatchStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        var batch = eFilingService.createAndSubmitBatch();
                        if (batch != null) {
                            log.info("eFiling batch submitted: {}", batch.getBatchNumber());
                            chunkContext.getStepContext()
                                    .getStepExecution()
                                    .getJobExecution()
                                    .getExecutionContext()
                                    .put("batchId", batch.getId().toString());
                        } else {
                            log.info("No SAR reports to submit in this run");
                        }
                    } catch (Exception e) {
                        log.error("Batch submission failed: {}", e.getMessage(), e);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * Step 3: Poll FinCEN for acknowledgments.
     */
    @Bean
    public Step pollAcknowledgmentStep() {
        return new StepBuilder("pollAcknowledgmentStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    try {
                        eFilingService.pollForAcknowledgments();
                        log.info("Acknowledgment polling completed");
                    } catch (Exception e) {
                        log.error("Acknowledgment polling failed: {}", e.getMessage(), e);
                    }
                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public RepositoryItemReader<SarReport> approvedSarReportReader() {
        return new RepositoryItemReaderBuilder<SarReport>()
                .name("approvedSarReportReader")
                .repository(sarReportRepository)
                .methodName("findByStatus")
                .arguments(List.of(SarStatus.APPROVED))
                .sorts(Map.of("createdAt", Sort.Direction.ASC))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<SarReport, SarReport> sarValidationProcessor() {
        return report -> {
            // Pre-validation filter: ensure minimum data completeness
            if (report.getSubjects() == null || report.getSubjects().isEmpty()) {
                log.warn("SAR {} skipped: no subjects", report.getReportNumber());
                return null; // null = skip this item
            }
            if (report.getNarrative() == null || report.getNarrative().isBlank()) {
                log.warn("SAR {} skipped: no narrative", report.getReportNumber());
                return null;
            }
            return report;
        };
    }

    @Bean
    public ItemWriter<SarReport> sarValidationWriter() {
        return items -> {
            // Items that passed validation are logged
            for (SarReport report : items) {
                log.debug("SAR {} passed pre-submission validation", report.getReportNumber());
            }
        };
    }

    @Bean
    public JobExecutionListener eFilingJobListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("FinCEN SAR eFiling batch job started - JobId: {}", jobExecution.getJobId());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("FinCEN SAR eFiling batch job completed - Status: {}, Duration: {}ms",
                        jobExecution.getStatus(),
                        jobExecution.getEndTime() != null && jobExecution.getStartTime() != null
                                ? java.time.Duration.between(jobExecution.getStartTime(),
                                jobExecution.getEndTime()).toMillis()
                                : "N/A");
            }
        };
    }
}
