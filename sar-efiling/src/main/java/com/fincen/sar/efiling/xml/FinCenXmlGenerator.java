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
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates FinCEN BSA SAR XML batch files per EFL_SARXBatchSchema.xsd v1.2.
 * All SeqNum values are globally unique across the document as required by the schema.
 */
@Slf4j
@Component
public class FinCenXmlGenerator {

    private static final String FINCEN_NS = "www.fincen.gov/base";
    private static final String SCHEMA_LOCATION =
            "www.fincen.gov/base https://www.fincen.gov/system/files/schema/base/EFL_SARXBatchSchema.xsd";
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

    public String generateBatchXml(EFilingBatch batch, List<SarReport> sarReports) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // Global SeqNum counter — ALL SeqNum attrs must be unique per EFilingBatchXML unique constraint
        AtomicLong seq = new AtomicLong(1);

        // Count totals for required root attributes
        long totalAmount = sarReports.stream()
                .filter(r -> r.getTotalSuspiciousAmount() != null && !Boolean.TRUE.equals(r.getNoAmountInvolved()))
                .mapToLong(r -> r.getTotalSuspiciousAmount().longValue())
                .sum();

        long partyCount = sarReports.stream().mapToLong(this::countParties).sum();
        long activityCount = sarReports.size();

        // Root: EFilingBatchXML with required attributes
        Element root = doc.createElementNS(FINCEN_NS, "EFilingBatchXML");
        root.setAttribute("xmlns", FINCEN_NS);
        root.setAttribute("xmlns:xsi", XSI_NS);
        root.setAttribute("xsi:schemaLocation", SCHEMA_LOCATION);
        root.setAttribute("TotalAmount", String.valueOf(totalAmount));
        root.setAttribute("PartyCount", String.valueOf(partyCount));
        root.setAttribute("ActivityCount", String.valueOf(activityCount));
        root.setAttribute("ActivityAttachmentCount", "0");
        root.setAttribute("AttachmentCount", "0");
        doc.appendChild(root);

        // Required fixed element
        createElement(doc, root, "FormTypeCode", "SARX");

        // Each SAR becomes an Activity element directly under root
        for (SarReport report : sarReports) {
            buildActivity(doc, root, report, seq);
        }

        return documentToString(doc);
    }

    private long countParties(SarReport report) {
        long count = 4; // Transmitter + TransmitterContact + FilingInstitution + FIWhereActivityOccurred
        if (report.getContactOfficeName() != null || report.getContactPhone() != null) count++;
        count += report.getBranches().size();
        count += report.getSubjects().size();
        return count;
    }

    private void buildActivity(Document doc, Element parent, SarReport report, AtomicLong seq) {
        Element activity = createElement(doc, parent, "Activity");
        activity.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));

        // EFilingPriorDocumentNumber and FilingDateText from base ActivityType
        createElement(doc, activity, "EFilingPriorDocumentNumber",
                report.isAmendment() ? report.getPriorBsaIdentifier() : "0");
        if (report.getFilingDate() != null) {
            createElement(doc, activity, "FilingDateText",
                    report.getFilingDate().format(DATE_FORMAT));
        }

        // ActivityAssociation (required, must come after base elements, before Parties)
        buildActivityAssociation(doc, activity, report, seq);

        // Parties — minimum 6 required by schema
        buildTransmitterParty(doc, activity, seq);
        buildTransmitterContactParty(doc, activity, seq); // always required (one of the 6 mandatory roles)
        buildFilingInstitutionParty(doc, activity, report, seq);
        if (report.getContactOfficeName() != null || report.getContactPhone() != null) {
            buildContactOfficeParty(doc, activity, report, seq);
        }
        buildFiWhereActivityOccurredParty(doc, activity, report, seq);

        for (SarBranch branch : report.getBranches()) {
            buildBranchParty(doc, activity, branch, seq);
        }

        for (SarSubject subject : report.getSubjects()) {
            buildSubjectParty(doc, activity, subject, report, seq);
        }

        // SuspiciousActivity (required)
        buildSuspiciousActivity(doc, activity, report, seq);

        // ActivityNarrativeInformation (required by schema, max 5 chunks)
        buildNarrative(doc, activity, report.getNarrative(), seq);
    }

    private void buildActivityAssociation(Document doc, Element activity, SarReport report, AtomicLong seq) {
        Element assoc = createElement(doc, activity, "ActivityAssociation");
        assoc.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));

        // Elements in schema order: ContinuingActivity, CorrectsAmends, InitialReport, JointReport
        // ValidateIndicatorType only accepts "Y" or "" — omit element for false (never write "N")
        if (Boolean.TRUE.equals(report.getContinuingActivity())) {
            createElement(doc, assoc, "ContinuingActivityReportIndicator", "Y");
        }
        if (Boolean.TRUE.equals(report.getCorrectsAmendsPrior())) {
            createElement(doc, assoc, "CorrectsAmendsPriorReportIndicator", "Y");
        }
        if (!Boolean.TRUE.equals(report.getCorrectsAmendsPrior())) {
            createElement(doc, assoc, "InitialReportIndicator", "Y");
        }
        if (Boolean.TRUE.equals(report.getJointReport())) {
            createElement(doc, assoc, "JointReportIndicator", "Y");
        }
    }

    private void buildTransmitterParty(Document doc, Element activity, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.TRANSMITTER.getCode()));

        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullName", transmitterName);

        if (!transmitterEin.isBlank()) {
            Element partyId = createElement(doc, party, "PartyIdentification");
            partyId.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyId, "PartyIdentificationNumberText", transmitterEin);
            createElement(doc, partyId, "PartyIdentificationTypeCode", "2"); // EIN
        }
    }

    private void buildTransmitterContactParty(Document doc, Element activity, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.TRANSMITTER_CONTACT.getCode()));

        String contactName = transmitterContactName.isBlank() ? transmitterName : transmitterContactName;
        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullName", contactName);

        if (!transmitterContactPhone.isBlank()) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, phone, "PhoneNumberText", transmitterContactPhone);
        }
        if (!transmitterContactEmail.isBlank()) {
            Element email = createElement(doc, party, "ElectronicAddress");
            email.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, email, "ElectronicAddressText", transmitterContactEmail);
        }
    }

    private void buildFilingInstitutionParty(Document doc, Element activity, SarReport report, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.FILING_INSTITUTION.getCode()));

        if (report.getFilingInstitutionName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullName", report.getFilingInstitutionName());
        }

        if (report.getFilingInstitutionEin() != null) {
            Element partyId = createElement(doc, party, "PartyIdentification");
            partyId.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyId, "PartyIdentificationNumberText", report.getFilingInstitutionEin());
            createElement(doc, partyId, "PartyIdentificationTypeCode", "2"); // EIN
        }
    }

    private void buildContactOfficeParty(Document doc, Element activity, SarReport report, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.CONTACT_FOR_ASSISTANCE.getCode()));

        if (report.getContactOfficeName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullName", report.getContactOfficeName());
        }
        if (report.getContactPhone() != null) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, phone, "PhoneNumberText", report.getContactPhone());
        }
        if (report.getContactEmail() != null) {
            Element email = createElement(doc, party, "ElectronicAddress");
            email.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, email, "ElectronicAddressText", report.getContactEmail());
        }
    }

    private void buildFiWhereActivityOccurredParty(Document doc, Element activity, SarReport report, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.FI_WHERE_ACTIVITY_OCCURRED.getCode()));

        Element partyName = createElement(doc, party, "PartyName");
        partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, partyName, "PartyNameTypeCode", "L");
        createElement(doc, partyName, "RawPartyFullName", report.getFilingInstitutionName());

        if (report.getFilingInstitutionEin() != null) {
            Element partyId = createElement(doc, party, "PartyIdentification");
            partyId.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyId, "PartyIdentificationNumberText", report.getFilingInstitutionEin());
            createElement(doc, partyId, "PartyIdentificationTypeCode", "2");
        }
    }

    private void buildBranchParty(Document doc, Element activity, SarBranch branch, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.BRANCH_WHERE_ACTIVITY_OCCURRED.getCode()));

        if (branch.getBranchName() != null) {
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyName, "PartyNameTypeCode", "L");
            createElement(doc, partyName, "RawPartyFullName", branch.getBranchName());
        }

        buildAddress(doc, party, branch.getAddress(), branch.getCity(),
                branch.getState(), branch.getZipCode(), branch.getCountry(), seq);
    }

    private void buildSubjectParty(Document doc, Element activity, SarSubject subject,
                                    SarReport report, AtomicLong seq) {
        Element party = createElement(doc, activity, "Party");
        party.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        createElement(doc, party, "ActivityPartyTypeCode",
                String.valueOf(PartyTypeCode.SUBJECT.getCode()));

        // Unknown subject flag (base PartyType element)
        if (Boolean.TRUE.equals(subject.getIsUnknown())) {
            createElement(doc, party, "AllCriticalSubjectInformationUnavailableIndicator", "Y");
        }

        // Entity indicator
        if (Boolean.TRUE.equals(subject.getIsEntity())) {
            createElement(doc, party, "PartyAsEntityOrganizationIndicator", "Y");
        }

        // Extension: PartyName
        if (!Boolean.TRUE.equals(subject.getIsUnknown())) {
            int nameSeq = 1;
            Element partyName = createElement(doc, party, "PartyName");
            partyName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, partyName, "PartyNameTypeCode", "L");

            if (Boolean.TRUE.equals(subject.getIsEntity())) {
                createElement(doc, partyName, "RawPartyFullName",
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
                dbaName.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
                createElement(doc, dbaName, "PartyNameTypeCode", "DBA");
                createElement(doc, dbaName, "RawPartyFullName", subject.getDoingBusinessAs());
            }
        }

        // Address
        buildAddress(doc, party, subject.getAddress(), subject.getCity(),
                subject.getState(), subject.getZipCode(), subject.getCountry(), seq);

        // Phone
        if (subject.getPhoneNumber() != null) {
            Element phone = createElement(doc, party, "PhoneNumber");
            phone.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, phone, "PhoneNumberText", subject.getPhoneNumber());
        }

        // PartyIdentification
        if (subject.getIdType() != null && subject.getIdNumber() != null) {
            Element partyId = createElement(doc, party, "PartyIdentification");
            partyId.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            // Element order per PartyIdentificationType restriction: country, state, numberText, typeCode
            if (subject.getIdIssueCountry() != null) {
                createElement(doc, partyId, "OtherIssuerCountryText", subject.getIdIssueCountry());
            }
            if (subject.getIdIssueState() != null) {
                createElement(doc, partyId, "OtherIssuerStateText", subject.getIdIssueState());
            }
            createElement(doc, partyId, "PartyIdentificationNumberText", subject.getIdNumber());
            createElement(doc, partyId, "PartyIdentificationTypeCode",
                    String.valueOf(subject.getIdType().getCode()));
        }

        // Email
        if (subject.getEmail() != null) {
            Element email = createElement(doc, party, "ElectronicAddress");
            email.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, email, "ElectronicAddressText", subject.getEmail());
        }

        // Occupation
        if (subject.getOccupation() != null || subject.getNaicsCode() != null) {
            Element occup = createElement(doc, party, "PartyOccupationBusiness");
            occup.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            if (subject.getOccupation() != null) {
                createElement(doc, occup, "OccupationBusinessText", subject.getOccupation());
            }
            if (subject.getNaicsCode() != null) {
                createElement(doc, occup, "NAICSCode", subject.getNaicsCode());
            }
        }

        // Date of birth (base PartyType element)
        if (subject.getDateOfBirth() != null) {
            createElement(doc, party, "IndividualBirthDateText",
                    subject.getDateOfBirth().format(DATE_FORMAT));
        }

        // Accounts linked to this subject
        List<SarAccount> subjectAccounts = new ArrayList<>();
        for (SarAccount account : report.getAccounts()) {
            if (subject.equals(account.getSubject())) {
                subjectAccounts.add(account);
            }
        }
        // PartyAccountAssociation wraps accounts
        for (SarAccount account : subjectAccounts) {
            buildPartyAccountAssociation(doc, party, account, seq);
        }
    }

    private void buildPartyAccountAssociation(Document doc, Element party, SarAccount account, AtomicLong seq) {
        Element assoc = createElement(doc, party, "PartyAccountAssociation");
        assoc.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));

        if (account.getAccountNumber() != null) {
            createElement(doc, assoc, "AccountNumberText", account.getAccountNumber());
        }
        if (Boolean.TRUE.equals(account.getAccountNumberClosed())) {
            createElement(doc, assoc, "AccountClosedIndicator", "Y");
        }
    }

    private void buildAddress(Document doc, Element parent, String address, String city,
                               String state, String zip, String country, AtomicLong seq) {
        if (address == null && city == null) return;
        Element addr = createElement(doc, parent, "Address");
        addr.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
        if (address != null) createElement(doc, addr, "RawStreetAddress1Text", address);
        if (city != null) createElement(doc, addr, "RawCityText", city);
        if (state != null) createElement(doc, addr, "RawStateCodeText", state);
        if (zip != null) createElement(doc, addr, "RawZIPCode", zip);
        createElement(doc, addr, "RawCountryCodeText", country != null ? country : "US");
    }

    private void buildSuspiciousActivity(Document doc, Element activity, SarReport report, AtomicLong seq) {
        Element suspActiv = createElement(doc, activity, "SuspiciousActivity");
        suspActiv.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));

        // Elements in schema order per SuspiciousActivityType restriction
        if (Boolean.TRUE.equals(report.getNoAmountInvolved())) {
            createElement(doc, suspActiv, "NoAmountInvolvedIndicator", "Y");
        }

        // SuspiciousActivityFromDateText is required
        if (report.getActivityFromDate() != null) {
            createElement(doc, suspActiv, "SuspiciousActivityFromDateText",
                    report.getActivityFromDate().format(DATE_FORMAT));
        } else {
            // Use today as fallback to satisfy required element
            createElement(doc, suspActiv, "SuspiciousActivityFromDateText",
                    java.time.LocalDate.now().format(DATE_FORMAT));
        }

        if (report.getActivityToDate() != null) {
            createElement(doc, suspActiv, "SuspiciousActivityToDateText",
                    report.getActivityToDate().format(DATE_FORMAT));
        }

        if (report.getTotalSuspiciousAmount() != null && !Boolean.TRUE.equals(report.getNoAmountInvolved())) {
            createElement(doc, suspActiv, "TotalSuspiciousAmountText",
                    String.valueOf(report.getTotalSuspiciousAmount().longValue()));
        }

        // SuspiciousActivityClassification (extension, required 1+)
        if (report.getActivityTypes() != null && !report.getActivityTypes().isEmpty()) {
            for (SarActivityType actType : report.getActivityTypes()) {
                Element classif = createElement(doc, suspActiv, "SuspiciousActivityClassification");
                classif.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));

                if (actType.getActivityTypeOther() != null) {
                    createElement(doc, classif, "OtherSuspiciousActivityTypeText",
                            actType.getActivityTypeOther());
                }
                createElement(doc, classif, "SuspiciousActivitySubtypeID",
                        String.valueOf(actType.getActivityTypeCode().getSubtypeId()));
                createElement(doc, classif, "SuspiciousActivityTypeID",
                        String.valueOf(actType.getActivityTypeCode().getTypeId()));
            }
        } else {
            // Schema requires at least one — use generic "Other" placeholder
            Element classif = createElement(doc, suspActiv, "SuspiciousActivityClassification");
            classif.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, classif, "SuspiciousActivitySubtypeID", "901");
            createElement(doc, classif, "SuspiciousActivityTypeID", "9");
        }
    }

    private void buildNarrative(Document doc, Element activity, String narrative, AtomicLong seq) {
        // Schema allows max 5 chunks of 4096 chars each (ActivityNarrativeInformation maxOccurs=5)
        if (narrative == null || narrative.isBlank()) {
            narrative = "No narrative provided.";
        }
        int maxChunkSize = 4096;
        int chunkNum = 1;
        for (int i = 0; i < narrative.length() && chunkNum <= 5; i += maxChunkSize, chunkNum++) {
            String chunk = narrative.substring(i, Math.min(i + maxChunkSize, narrative.length()));
            Element narrativeEl = createElement(doc, activity, "ActivityNarrativeInformation");
            narrativeEl.setAttribute("SeqNum", String.valueOf(seq.getAndIncrement()));
            createElement(doc, narrativeEl, "ActivityNarrativeSequenceNumber", String.valueOf(chunkNum));
            createElement(doc, narrativeEl, "ActivityNarrativeText", chunk);
        }
    }

    private Element createElement(Document doc, Element parent, String tagName) {
        Element element = doc.createElementNS(FINCEN_NS, tagName);
        parent.appendChild(element);
        return element;
    }

    private Element createElement(Document doc, Element parent, String tagName, String textContent) {
        Element element = doc.createElementNS(FINCEN_NS, tagName);
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
