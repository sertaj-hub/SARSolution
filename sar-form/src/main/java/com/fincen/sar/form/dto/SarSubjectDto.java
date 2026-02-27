package com.fincen.sar.form.dto;

import com.fincen.sar.core.enums.IdentificationTypeCode;
import com.fincen.sar.core.enums.SubjectRoleCode;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class SarSubjectDto {
    private UUID id;
    private Integer seqNum;
    private String caseSubjectId;
    private Boolean autoPopulated;
    private Boolean isEntity;
    private String lastName;
    private String firstName;
    private String middleName;
    private String suffix;
    private LocalDate dateOfBirth;
    private String entityName;
    private String doingBusinessAs;
    private IdentificationTypeCode idType;
    private String idNumber;
    private String idIssueState;
    private String idIssueCountry;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phoneNumber;
    private String email;
    private String occupation;
    private String naicsCode;
    private SubjectRoleCode roleCode;
    private String roleOtherDescription;
    private Boolean isUnknown;
    private Boolean stillEmployed;
    private Boolean hasRelationshipToAccount;
}
