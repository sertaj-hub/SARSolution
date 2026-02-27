package com.fincen.sar.core.domain;

import com.fincen.sar.core.enums.IdentificationTypeCode;
import com.fincen.sar.core.enums.SubjectRoleCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

/**
 * SAR Subject (Party type 23) - corresponds to FinCEN SAR Part I (Items 3-28).
 * Represents individuals or entities involved in suspicious activity.
 * Up to 999 subjects per SAR activity per BSA XML schema.
 */
@Entity
@Table(name = "sar_subjects")
@Audited
@Getter
@Setter
public class SarSubject extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sar_report_id", nullable = false)
    private SarReport sarReport;

    // Sequence number for XML generation (1-999)
    @Column(name = "seq_num")
    private Integer seqNum;

    // Source auto-population
    @Column(name = "case_subject_id")
    private String caseSubjectId;

    @Column(name = "auto_populated")
    private Boolean autoPopulated = false;

    // --- Identity (Part I, Items 3-9) ---

    @Column(name = "is_entity")
    private Boolean isEntity = false; // true = organization, false = individual

    // Individual fields
    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "suffix")
    private String suffix;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Entity fields
    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "doing_business_as")
    private String doingBusinessAs;

    // --- Identification (Part I, Items 10-12) ---

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type")
    private IdentificationTypeCode idType;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "id_issue_state")
    private String idIssueState; // 2-letter state code

    @Column(name = "id_issue_country")
    private String idIssueCountry; // ISO country code

    // --- Contact (Part I, Items 13-19) ---

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "country")
    private String country = "US";

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "phone_extension")
    private String phoneExtension;

    @Column(name = "email")
    private String email;

    // --- Occupation / Business (Part I, Items 20-21) ---

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "naics_code")
    private String naicsCode;

    // --- Role (Part I, Item 22) ---

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code")
    private SubjectRoleCode roleCode;

    @Column(name = "role_other_description")
    private String roleOtherDescription;

    // --- Status flags (Part I, Items 23-28) ---

    @Column(name = "is_unknown")
    private Boolean isUnknown = false;

    @Column(name = "still_employed")
    private Boolean stillEmployed;

    @Column(name = "corrective_action")
    private String correctiveAction;

    // --- Relationship to account ---

    @Column(name = "has_relationship_to_account")
    private Boolean hasRelationshipToAccount = false;

    public String getDisplayName() {
        if (Boolean.TRUE.equals(isEntity)) {
            return entityName != null ? entityName : "Unknown Entity";
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName).append(" ");
        if (middleName != null) sb.append(middleName).append(" ");
        if (lastName != null) sb.append(lastName);
        return sb.toString().trim().isEmpty() ? "Unknown Individual" : sb.toString().trim();
    }
}
