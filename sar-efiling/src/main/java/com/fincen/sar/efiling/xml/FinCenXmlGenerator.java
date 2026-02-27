package com.fincen.sar.efiling.xml;

import com.fincen.sar.core.domain.*;
import com.fincen.sar.core.enums.PartyTypeCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates FinCEN BSA SAR XML batch files per BSA XML 2.0 schema.
 * Schema: https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd
 * Reference: https://bsaefiling.fincen.gov/docs/XMLUserGuide_FinCENSAR.pdf
 */
@Slf4j
@Component
public class FinCenXmlGenerator {

    private static final String SCHEMA_LOCATION = "https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${sar.efiling.transmitter.name:SAR Solution System}")
    private String transmitterName;

    @Value("${sar.efiling.transmitter.ein:}")
    private String transmitterEin;

    @Value("${sar.efiling.transmitter.contact-name:}")
    private String transmitterContactName;

    @Value("${sar.efiling.transmitter.contact-phone:}")
    private String transmitterContactPhone;

    @Value("${sar.efiling.transmitter.contact-email:}")
    private String transmitterContactEmail;

    /**
     * Generate a complete FinCEN BSA E-Filing batch XML document.
     *
     * @param batch       The EFilingBatch entity
     * @param sarReports  The SAR reports to include
     * @return XML content as String
     */
    public String generateBatchXml(EFilingBatch batch, List<SarReport> sarReports) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Root: EFilingBatchXML
        Element root = doc.createElement("EFilingBatchXML");
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xsi:noNamespaceSchemaLocation", SCHEMA_LOCATION);
        root.setAttribute("SeqNum", "1");
        root.setAttribute("StatusCode", "A");
        doc.appendChild(root);

        // EFilingSubmissionXML
        Element submission = doc.createElement("EFilingSubmissionXML");
        submission.setAttribute("SeqNum", "1");
        root.appendChild(submission);

        // Each SAR becomes an EFilingActivityXML
        for (int i = 0; i < sarReports.size(); i++) {
            SarReport report = sarReports.get(i);
            Element activityWrapper = doc.createElement("EFilingActivityXML");
            activityWrapper.setAttribute("SeqNum", String.valueOf(i + 1));
            submission.appendChild(activityWrapper);

            buildActivity(doc, activityWrapper, report, i + 1);
        }

        return documentToString(doc);
    }

    private void buildActivity(Document doc, Element parent, SarReport report, int activitySeq) {
        Element activity = createElement(doc, parent, "Activity");
        activity.setAttribute("SeqNum", String.valueOf(activitySeq));

        // Prior document number (0 if new, BSA ID if amendment)
        createElement(doc, activity, "EFilingPriorDocumentNumber",
                report.isAmendment() ? report.getPriorBsaIdentifier() : "0");

        // Filing date
        if (report.getFilingDate() != null) {
            createElement(doc, activity, "FilingDateText",
                    report.getFilingDate().format(DATE_FORMAT));
        }

        // ActivityAssociation
        Element assoc = createElement(doc, activity, "ActivityAssociation");
        assoc.setAttribute("SeqNum", "1");
        createElement(doc, assoc, "CorrectsAmendsPriorReportIndicator",
                Boolean.TRUE.equals(report.getCorrectsAmendsPrior()) ? "Y" : "N");
        createElement(doc, assoc, "FinancialInstitutionNotedSuspiciousActivityIndicator",
                Boolean.TRUE.equals(report.getFiNotedSuspiciousActivity()) ? "Y" : "N");
        if (Boolean.TRUE.equals(report.getContinuingActivity())) {
            createElement(doc, assoc, "ContinuingActivityReportIndicator", "Y");
        }
        if (Boolean.TRUE.equals(report.getJointReport())) {
            createElement(doc, assoc, "JointReportIndicator", "Y");
        }

        // --- Parties ---
        AtomicInteger partySeq = new AtomicInteger(1);

        // Party 1: Transmitter (type 8)
        buildTransmitterParty(doc, activity, partySeq.getAndIncrement());

        // Party 2: Transmitter Contact (type 9)
        buildTransmitterContactParty(doc, activity, partySeq.getAndIncrement());

        // Party 3: Filing Institution (type 35)
        buildFilingInstitutionParty(doc, activity, report, partySeq.getAndIncrement());

        // Party 4: Designated Contact Office (type 46)
        buildContactOfficeParty(doc, activity, report, partySeq.getAndIncrement());

        // Party: FI where activity occurred (type 33)
        buildFiWhereActivityOccurredParty(doc, activity, report, partySeq.getAndIncrement());

        // Parties: Branch where activity occurred (type 34)
        for (SarBranch branch : report.getBranches()) {
            buildBranchParty(doc, activity, branch, partySeq.getAndIncrement());
        }

        // Parties: Subjects (type 23) - up to 999
        for (SarSubject subject : report.getSubjects()) {
            buildSubjectParty(doc, activity, subject, report, partySeq.getAndIncrement());
        }

        // SuspiciousActivity
        buildSuspiciousActivity(doc, activity, report);

        // ActivityNarrativeInformation
        if (report.getNarrative() != null && !report.getNarrative().isBlank()) {
            buildNarrative(doc, activity, report.getNarrative());
        }
    }

    private void buildTransmitterParty(Document doc, Element activity, int seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.TRANSMITTER.getCode()));

        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", "1");
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullLegalName", transmitterName);
        createElement(doc, party, "TINTypeCode", "2"); // EIN
        if (!transmitterEin.isBlank()) {
            createElement(doc, party, "TIN", transmitterEin);
        }
    }

    private void buildTransmitterContactParty(Document doc, Element activity, int seq) {
        if (transmitterContactName.isBlank()) return;
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.TRANSMITTER_CONTACT.getCode()));

        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", "1");
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullLegalName", transmitterContactName);

        if (!transmitterContactPhone.isBlank()) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", "1");
            createElement(doc, phone, "PhoneNumberText", transmitterContactPhone);
        }
        if (!transmitterContactEmail.isBlank()) {
            createElement(doc, party, "PrimaryEmailAddress", transmitterContactEmail);
        }
    }

    private void buildFilingInstitutionParty(Document doc, Element activity, SarReport report, int seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.FILING_INSTITUTION.getCode()));

        if (report.getFilingInstitutionName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", "1");
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullLegalName", report.getFilingInstitutionName());
        }

        if (report.getFilingInstitutionEin() != null) {
            createElement(doc, party, "TINTypeCode", "2"); // EIN
            createElement(doc, party, "TIN", report.getFilingInstitutionEin());
        }
    }

    private void buildContactOfficeParty(Document doc, Element activity, SarReport report, int seq) {
        if (report.getContactOfficeName() == null && report.getContactPhone() == null) return;
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.DESIGNATED_CONTACT_OFFICE.getCode()));

        if (report.getContactOfficeName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", "1");
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullLegalName", report.getContactOfficeName());
        }
        if (report.getContactPhone() != null) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", "1");
            createElement(doc, phone, "PhoneNumberText", report.getContactPhone());
        }
        if (report.getContactEmail() != null) {
            createElement(doc, party, "PrimaryEmailAddress", report.getContactEmail());
        }
    }

    private void buildFiWhereActivityOccurredParty(Document doc, Element activity, SarReport report, int seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.FI_WHERE_ACTIVITY_OCCURRED.getCode()));

        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", "1");
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullLegalName", report.getFilingInstitutionName());

        if (report.getFilingInstitutionEin() != null) {
            createElement(doc, party, "TINTypeCode", "2");
            createElement(doc, party, "TIN", report.getFilingInstitutionEin());
        }
    }

    private void buildBranchParty(Document doc, Element activity, SarBranch branch, int seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.BRANCH_WHERE_ACTIVITY_OCCURRED.getCode()));

        if (branch.getBranchName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", "1");
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullLegalName", branch.getBranchName());
        }
        if (branch.getRssdNumber() != null) {
            createElement(doc, party, "RssdNumber", branch.getRssdNumber());
        }

        buildAddress(doc, party, branch.getAddress(), branch.getCity(),
                branch.getState(), branch.getZipCode(), branch.getCountry(), 1);
    }

    private void buildSubjectParty(Document doc, Element activity, SarSubject subject,
                                    SarReport report, int seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.SUBJECT.getCode()));

        // Unknown subject flag
        if (Boolean.TRUE.equals(subject.getIsUnknown())) {
            createElement(doc, party, "AllCriticalSubjectInformationUnavailableIndicator", "Y");
        }

        // Party name
        int nameSeq = 1;
        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", String.valueOf(nameSeq++));
        createElement(doc, partyName, "PartyNameTypeCode", "L");

        if (Boolean.TRUE.equals(subject.getIsEntity())) {
            createElement(doc, partyName, "RawPartyFullLegalName",
                    subject.getEntityName() != null ? subject.getEntityName() : "UNKNOWN ENTITY");
        } else {
            if (subject.getLastName() != null) {
                createElement(doc, partyName, "RawEntityIndividualLastName", subject.getLastName());
            }
            if (subject.getFirstName() != null) {
                createElement(doc, partyName, "RawIndividualFirstName", subject.getFirstName());
            }
            if (subject.getMiddleName() != null) {
                createElement(doc, partyName, "RawIndividualMiddleName", subject.getMiddleName());
            }
            if (subject.getSuffix() != null) {
                createElement(doc, partyName, "RawIndividualNameSuffixText", subject.getSuffix());
            }
        }

        // DBA name
        if (subject.getDoingBusinessAs() != null) {
            Element dbaName = createElement(doc, party, "PartyName");
            dbaName.setAttribute("SeqNum", String.valueOf(nameSeq));
            createElement(doc, dbaName, "PartyNameTypeCode", "DBA");
            createElement(doc, dbaName, "RawPartyFullLegalName", subject.getDoingBusinessAs());
        }

        // Date of birth
        if (subject.getDateOfBirth() != null) {
            createElement(doc, party, "IndividualBirthDateText",
                    subject.getDateOfBirth().format(DATE_FORMAT));
        }

        // Identification
        if (subject.getIdType() != null && subject.getIdNumber() != null) {
            Element partyId = createElement(doc, party, "PartyIdentification");
            partyId.setAttribute("SeqNum", "1");
            createElement(doc, partyId, "PartyIdentificationTypeCode",
                    String.valueOf(subject.getIdType().getCode()));
            createElement(doc, partyId, "OtherIssuerStateCode",
                    subject.getIdIssueState() != null ? subject.getIdIssueState() : "");
            createElement(doc, partyId, "OtherIssuerCountryCode",
                    subject.getIdIssueCountry() != null ? subject.getIdIssueCountry() : "US");
            createElement(doc, partyId, "TIN", subject.getIdNumber());
        }

        // Address
        buildAddress(doc, party, subject.getAddress(), subject.getCity(),
                subject.getState(), subject.getZipCode(), subject.getCountry(), 1);

        // Phone
        if (subject.getPhoneNumber() != null) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", "1");
            createElement(doc, phone, "PhoneNumberText", subject.getPhoneNumber());
        }

        // Email
        if (subject.getEmail() != null) {
            createElement(doc, party, "PrimaryEmailAddress", subject.getEmail());
        }

        // Occupation
        if (subject.getOccupation() != null) {
            createElement(doc, party, "PrimaryOccupationText", subject.getOccupation());
        }
        if (subject.getNaicsCode() != null) {
            createElement(doc, party, "NAICSCode", subject.getNaicsCode());
        }

        // Role
        if (subject.getRoleCode() != null) {
            createElement(doc, party, "PartyRoleCode", subject.getRoleCode().getCode());
        }

        // Accounts linked to this subject
        int acctSeq = 1;
        for (SarAccount account : report.getAccounts()) {
            if (subject.equals(account.getSubject())) {
                buildAccount(doc, party, account, acctSeq++);
            }
        }

        // Employment status
        if (subject.getStillEmployed() != null) {
            createElement(doc, party, "SubjectStillEmployedIndicator",
                    Boolean.TRUE.equals(subject.getStillEmployed()) ? "Y" : "N");
        }
    }

    private void buildAccount(Document doc, Element party, SarAccount account, int seq) {
        Element accountEl = createElement(doc, party, "Account");
        accountEl.setAttribute("SeqNum", String.valueOf(seq));

        if (account.getAccountNumber() != null) {
            createElement(doc, accountEl, "AccountNumberText", account.getAccountNumber());
        }
        if (Boolean.TRUE.equals(account.getAccountNumberClosed())) {
            createElement(doc, accountEl, "ClosedIndicator", "Y");
        }
        if (account.getInstitutionName() != null) {
            createElement(doc, accountEl, "FinancialInstitutionName", account.getInstitutionName());
        }
        if (account.getRoutingNumber() != null) {
            Element partyId = createElement(doc, accountEl, "AccountFinancialInstitutionIdentification");
            partyId.setAttribute("SeqNum", "1");
            createElement(doc, partyId, "FinancialInstitutionIdentificationTypeCode", "14"); // Routing number
            createElement(doc, partyId, "FinancialInstitutionRoutingNumber", account.getRoutingNumber());
        }
        if (Boolean.TRUE.equals(account.getActionAccountClosed())) {
            createElement(doc, accountEl, "ClosedAccountIndicator", "Y");
        } else if (Boolean.TRUE.equals(account.getActionAccountFrozen())) {
            createElement(doc, accountEl, "FrozenAccountIndicator", "Y");
        } else if (Boolean.TRUE.equals(account.getNoActionTaken())) {
            createElement(doc, accountEl, "NoActionTakenIndicator", "Y");
        }
    }

    private void buildAddress(Document doc, Element parent, String address, String city,
                               String state, String zip, String country, int seq) {
        if (address == null && city == null) return;
        Element addr = createElement(doc, parent, "Address");
        addr.setAttribute("SeqNum", String.valueOf(seq));
        if (address != null) createElement(doc, addr, "RawStreetAddress1Text", address);
        if (city != null) createElement(doc, addr, "RawCityText", city);
        if (state != null) createElement(doc, addr, "StateName", state);
        if (zip != null) createElement(doc, addr, "ZIPCode", zip);
        createElement(doc, addr, "CountryCodeText", country != null ? country : "US");
    }

    private void buildSuspiciousActivity(Document doc, Element activity, SarReport report) {
        if (report.getActivityTypes() == null || report.getActivityTypes().isEmpty()) return;

        Element suspActiv = createElement(doc, activity, "SuspiciousActivity");
        suspActiv.setAttribute("SeqNum", "1");

        if (report.getActivityFromDate() != null) {
            createElement(doc, suspActiv, "SuspiciousActivityFromDateText",
                    report.getActivityFromDate().format(DATE_FORMAT));
        }
        if (report.getActivityToDate() != null) {
            createElement(doc, suspActiv, "SuspiciousActivityToDateText",
                    report.getActivityToDate().format(DATE_FORMAT));
        }
        if (report.getTotalSuspiciousAmount() != null && !Boolean.TRUE.equals(report.getNoAmountInvolved())) {
            createElement(doc, suspActiv, "TotalSuspiciousAmountText",
                    String.valueOf(report.getTotalSuspiciousAmount().longValue()));
        }
        if (Boolean.TRUE.equals(report.getNoAmountInvolved())) {
            createElement(doc, suspActiv, "NoAmountInvolvedIndicator", "Y");
        }

        // Suspicious activity subtypes
        int subtypeSeq = 1;
        for (SarActivityType actType : report.getActivityTypes()) {
            Element subtype = createElement(doc, suspActiv, "SuspiciousActivitySubtype");
            subtype.setAttribute("SeqNum", String.valueOf(subtypeSeq++));
            createElement(doc, subtype, "SuspiciousActivitySubtypeID",
                    String.valueOf(actType.getActivityTypeCode().getCode()));
            if (actType.getActivityTypeOther() != null) {
                createElement(doc, subtype, "OtherSuspiciousActivityDesc", actType.getActivityTypeOther());
            }
            if (actType.getProductInstrumentDescription() != null) {
                createElement(doc, subtype, "ProductInstrumentDescription",
                        actType.getProductInstrumentDescription());
            }
        }
    }

    private void buildNarrative(Document doc, Element activity, String narrative) {
        // Split narrative into 4096-char chunks if needed
        int maxChunkSize = 4096;
        int seqNum = 1;
        for (int i = 0; i < narrative.length(); i += maxChunkSize) {
            String chunk = narrative.substring(i, Math.min(i + maxChunkSize, narrative.length()));
            Element narrativeEl = createElement(doc, activity, "ActivityNarrativeInformation");
            narrativeEl.setAttribute("SeqNum", String.valueOf(seqNum));
            createElement(doc, narrativeEl, "ActivityNarrativeSequenceNumber", String.valueOf(seqNum));
            createElement(doc, narrativeEl, "ActivityNarrativeText", chunk);
            seqNum++;
        }
    }

    private Element createElement(Document doc, Element parent, String tagName) {
        Element element = doc.createElement(tagName);
        parent.appendChild(element);
        return element;
    }

    private Element createElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        if (textContent != null) {
            element.setTextContent(textContent);
        }
        parent.appendChild(element);
        return element;
    }

    private String documentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
