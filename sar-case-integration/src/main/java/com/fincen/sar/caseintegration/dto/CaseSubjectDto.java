package com.fincen.sar.caseintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseSubjectDto {
    private String subjectId;
    private Boolean isEntity;
    // Individual
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
    private LocalDate dateOfBirth;
    // Entity
    private String entityName;
    private String doingBusinessAs;
    // ID
    private String idType;
    private String idNumber;
    private String idIssueState;
    private String idIssueCountry;
    // Contact
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private String phoneNumber;
    private String email;
    // Employment
    private String occupation;
    private String naicsCode;
    private String role;
    private String roleOther;
}
