package com.fincen.sar.form.validator;

import com.fincen.sar.core.domain.SarReport;
import com.fincen.sar.core.exception.SarValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates SAR report completeness per FinCEN SAR form requirements.
 * Must pass before status transitions (e.g., DRAFT -> IN_REVIEW, IN_REVIEW -> APPROVED).
 */
@Component
public class SarFormValidator {

    /**
     * Validate that a SAR report is complete enough for review submission.
     */
    public void validateForReview(SarReport report) {
        List<String> violations = new ArrayList<>();

        // Part I: Subject validation
        if (report.getSubjects() == null || report.getSubjects().isEmpty()) {
            violations.add("Part I: At least one subject is required");
        } else {
            for (int i = 0; i < report.getSubjects().size(); i++) {
                var subject = report.getSubjects().get(i);
                if (Boolean.TRUE.equals(subject.getIsEntity())) {
                    if (subject.getEntityName() == null || subject.getEntityName().isBlank()) {
                        violations.add("Part I, Subject " + (i + 1) + ": Entity name is required");
                    }
                } else if (!Boolean.TRUE.equals(subject.getIsUnknown())) {
                    if (subject.getLastName() == null || subject.getLastName().isBlank()) {
                        violations.add("Part I, Subject " + (i + 1) + ": Last name is required for individuals");
                    }
                }
            }
        }

        // Part II: Activity validation
        if (report.getActivityFromDate() == null) {
            violations.add("Part II: Suspicious activity from date is required");
        }
        if (report.getActivityToDate() == null) {
            violations.add("Part II: Suspicious activity to date is required");
        }
        if (report.getActivityFromDate() != null && report.getActivityToDate() != null
                && report.getActivityFromDate().isAfter(report.getActivityToDate())) {
            violations.add("Part II: Activity from date cannot be after to date");
        }
        if ((report.getTotalSuspiciousAmount() == null || report.getTotalSuspiciousAmount().signum() <= 0)
                && !Boolean.TRUE.equals(report.getNoAmountInvolved())) {
            violations.add("Part II: Total suspicious amount or 'no amount involved' must be specified");
        }
        if (report.getActivityTypes() == null || report.getActivityTypes().isEmpty()) {
            violations.add("Part II: At least one type of suspicious activity must be selected");
        }

        // Part III: Filing institution validation
        if (report.getFilingInstitutionName() == null || report.getFilingInstitutionName().isBlank()) {
            violations.add("Part III: Filing institution name is required");
        }

        // Part V: Narrative validation
        if (report.getNarrative() == null || report.getNarrative().trim().length() < 17) {
            violations.add("Part V: Narrative is required and must be at least 17 characters");
        }

        if (!violations.isEmpty()) {
            throw new SarValidationException(violations);
        }
    }

    /**
     * Validate that a SAR report is fully complete and approved for eFiling.
     */
    public void validateForFiling(SarReport report) {
        validateForReview(report);
        List<String> violations = new ArrayList<>();

        if (report.getFilingDate() == null) {
            violations.add("Filing date is required for eFiling submission");
        }
        if (report.getContactPhone() == null || report.getContactPhone().isBlank()) {
            violations.add("Contact phone number is required for eFiling");
        }
        if (report.getFilingInstitutionEin() == null || report.getFilingInstitutionEin().isBlank()) {
            violations.add("Filing institution EIN is required for eFiling");
        }
        if (report.getBranches() == null || report.getBranches().isEmpty()) {
            violations.add("At least one branch where activity occurred is required for eFiling");
        }

        // Amendment validation
        if (Boolean.TRUE.equals(report.getCorrectsAmendsPrior()) && report.getPriorBsaIdentifier() == null) {
            violations.add("Prior BSA Identifier is required for amendment filings");
        }

        if (!violations.isEmpty()) {
            throw new SarValidationException(violations);
        }
    }
}
