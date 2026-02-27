package com.fincen.sar.form.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SarBranchDto {
    private UUID id;
    private Integer seqNum;
    private String branchName;
    private String rssdNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private Boolean isPrimary;
}
