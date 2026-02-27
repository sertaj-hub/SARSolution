package com.fincen.sar.caseintegration.service;

import com.fincen.sar.caseintegration.client.CaseManagementClient;
import com.fincen.sar.caseintegration.dto.CaseAccountDto;
import com.fincen.sar.caseintegration.dto.CaseDto;
import com.fincen.sar.caseintegration.dto.CaseSubjectDto;
import com.fincen.sar.caseintegration.dto.CaseTransactionDto;
import com.fincen.sar.caseintegration.mapper.CaseToSarMapper;
import com.fincen.sar.core.domain.*;
import com.fincen.sar.core.enums.ActivityTypeCode;
import com.fincen.sar.core.exception.SarException;
import com.fincen.sar.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates auto-population of SAR form from AML case management data.
 * Supports subjects, accounts, and transactions from connected case systems.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CasePopulationService {

    private final CaseManagementClient caseManagementClient;
    private final CaseToSarMapper mapper;
    private final SarReportRepository sarReportRepository;
    private final SarSubjectRepository subjectRepository;
    private final SarAccountRepository accountRepository;
    private final SarTransactionRepository transactionRepository;

    /**
     * Auto-populate a SAR report from an AML case.
     * Fetches case data and maps subjects, accounts, and transactions.
     *
     * @param sarReportId SAR report ID to populate
     * @param caseId      AML case ID to pull data from
     * @return Summary of populated data
     */
    @Transactional
    public PopulationResult populateFromCase(java.util.UUID sarReportId, String caseId) {
        SarReport report = sarReportRepository.findById(sarReportId)
                .orElseThrow(() -> new SarException("SAR_NOT_FOUND", "SAR Report not found: " + sarReportId));

        log.info("Auto-populating SAR {} from case {} in system {}",
                report.getReportNumber(), caseId, caseManagementClient.getSystemName());

        CaseDto caseData = caseManagementClient.getCaseById(caseId)
                .orElseThrow(() -> new SarException("CASE_NOT_FOUND",
                        "Case not found in " + caseManagementClient.getSystemName() + ": " + caseId));

        // Set case reference
        report.setCaseId(caseId);
        report.setCaseSystem(caseManagementClient.getSystemName());

        // Populate activity dates and amounts
        populateActivityInfo(report, caseData);

        // Map subjects (up to 999 per FinCEN spec)
        int subjectCount = populateSubjects(report, caseData);

        // Map accounts with subject linkage
        Map<String, SarSubject> subjectMap = buildSubjectMap(report.getSubjects());
        int accountCount = populateAccounts(report, caseData, subjectMap);

        // Map transactions with account linkage
        Map<String, SarAccount> accountMap = buildAccountMap(report.getAccounts());
        int txnCount = populateTransactions(report, caseData, accountMap);

        // Populate activity types from case
        populateActivityTypes(report, caseData);

        sarReportRepository.save(report);

        PopulationResult result = new PopulationResult(caseId, subjectCount, accountCount, txnCount);
        log.info("Auto-population complete for SAR {}: {} subjects, {} accounts, {} transactions",
                report.getReportNumber(), subjectCount, accountCount, txnCount);
        return result;
    }

    private void populateActivityInfo(SarReport report, CaseDto caseData) {
        if (caseData.getActivityFromDate() != null && report.getActivityFromDate() == null) {
            try {
                report.setActivityFromDate(LocalDate.parse(caseData.getActivityFromDate()));
            } catch (Exception ignored) {}
        }
        if (caseData.getActivityToDate() != null && report.getActivityToDate() == null) {
            try {
                report.setActivityToDate(LocalDate.parse(caseData.getActivityToDate()));
            } catch (Exception ignored) {}
        }
        if (caseData.getTotalSuspiciousAmount() != null && report.getTotalSuspiciousAmount() == null) {
            report.setTotalSuspiciousAmount(caseData.getTotalSuspiciousAmount());
        }
        if (caseData.getSuspiciousActivitySummary() != null && report.getNarrative() == null) {
            report.setNarrative(caseData.getSuspiciousActivitySummary());
        }
    }

    private int populateSubjects(SarReport report, CaseDto caseData) {
        if (caseData.getSubjects() == null || caseData.getSubjects().isEmpty()) return 0;

        AtomicInteger seqNum = new AtomicInteger(report.getSubjects().size() + 1);
        List<CaseSubjectDto> subjects = caseData.getSubjects().stream()
                .filter(s -> report.getSubjects().stream()
                        .noneMatch(existing -> s.getSubjectId().equals(existing.getCaseSubjectId())))
                .limit(999 - report.getSubjects().size())
                .toList();

        for (CaseSubjectDto subjectDto : subjects) {
            SarSubject subject = mapper.caseSubjectToSarSubject(subjectDto);
            subject.setSeqNum(seqNum.getAndIncrement());
            report.addSubject(subject);
        }
        return subjects.size();
    }

    private int populateAccounts(SarReport report, CaseDto caseData, Map<String, SarSubject> subjectMap) {
        if (caseData.getAccounts() == null || caseData.getAccounts().isEmpty()) return 0;

        AtomicInteger count = new AtomicInteger(0);
        for (CaseAccountDto accountDto : caseData.getAccounts()) {
            boolean exists = report.getAccounts().stream()
                    .anyMatch(a -> accountDto.getAccountId().equals(a.getCaseAccountId()));
            if (!exists) {
                SarAccount account = mapper.caseAccountToSarAccount(accountDto);
                if (accountDto.getSubjectId() != null) {
                    account.setSubject(subjectMap.get(accountDto.getSubjectId()));
                }
                report.addAccount(account);
                count.incrementAndGet();
            }
        }
        return count.get();
    }

    private int populateTransactions(SarReport report, CaseDto caseData, Map<String, SarAccount> accountMap) {
        if (caseData.getTransactions() == null || caseData.getTransactions().isEmpty()) return 0;

        AtomicInteger count = new AtomicInteger(0);
        for (CaseTransactionDto txDto : caseData.getTransactions()) {
            boolean exists = report.getTransactions().stream()
                    .anyMatch(t -> txDto.getTransactionId().equals(t.getCaseTransactionId()));
            if (!exists) {
                SarTransaction txn = mapper.caseTransactionToSarTransaction(txDto);
                if (txDto.getAccountId() != null) {
                    txn.setAccount(accountMap.get(txDto.getAccountId()));
                }
                report.addTransaction(txn);
                count.incrementAndGet();
            }
        }
        return count.get();
    }

    private void populateActivityTypes(SarReport report, CaseDto caseData) {
        if (caseData.getActivityTypes() == null) return;
        AtomicInteger seqNum = new AtomicInteger(report.getActivityTypes().size() + 1);
        for (String typeStr : caseData.getActivityTypes()) {
            try {
                ActivityTypeCode code = ActivityTypeCode.valueOf(
                        typeStr.toUpperCase().replace("-", "_").replace(" ", "_"));
                boolean exists = report.getActivityTypes().stream()
                        .anyMatch(at -> at.getActivityTypeCode() == code);
                if (!exists) {
                    SarActivityType activityType = new SarActivityType();
                    activityType.setActivityTypeCode(code);
                    activityType.setSeqNum(seqNum.getAndIncrement());
                    report.addActivityType(activityType);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Unknown activity type from case: {}", typeStr);
            }
        }
    }

    private Map<String, SarSubject> buildSubjectMap(List<SarSubject> subjects) {
        Map<String, SarSubject> map = new HashMap<>();
        for (SarSubject subject : subjects) {
            if (subject.getCaseSubjectId() != null) {
                map.put(subject.getCaseSubjectId(), subject);
            }
        }
        return map;
    }

    private Map<String, SarAccount> buildAccountMap(List<SarAccount> accounts) {
        Map<String, SarAccount> map = new HashMap<>();
        for (SarAccount account : accounts) {
            if (account.getCaseAccountId() != null) {
                map.put(account.getCaseAccountId(), account);
            }
        }
        return map;
    }

    public record PopulationResult(String caseId, int subjectCount, int accountCount, int transactionCount) {}
}
