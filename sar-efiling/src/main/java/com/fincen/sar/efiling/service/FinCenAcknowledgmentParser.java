package com.fincen.sar.efiling.service;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses FinCEN BSA E-Filing acknowledgment XML responses.
 * Extracts status, BSA identifiers, and error information.
 */
@Slf4j
public class FinCenAcknowledgmentParser {

    public static AcknowledgmentResult parse(String xml) {
        AcknowledgmentResult result = new AcknowledgmentResult();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            // Status from root StatusCode attribute
            String statusCode = getAttributeValue(doc, "EFilingBatchStatusXML", "StatusCode");
            result.setStatus(statusCode != null ? statusCode : "U");

            // Extract error codes and description
            String errorCodes = getTextContent(doc, "EFilingSubmissionStatusCode");
            String errorDesc = getTextContent(doc, "EFilingSubmissionStatusDescription");
            result.setErrorCodes(errorCodes);
            result.setErrorDescription(errorDesc);

            // Extract BSA IDs from each activity acknowledgment
            NodeList activityNodes = doc.getElementsByTagName("EFilingActivityXML");
            for (int i = 0; i < activityNodes.getLength(); i++) {
                org.w3c.dom.Element actEl = (org.w3c.dom.Element) activityNodes.item(i);
                String reportNum = getChildText(actEl, "ReportNumber");
                String bsaId = getChildText(actEl, "BSAIdentifier");
                if (reportNum != null && bsaId != null) {
                    result.addBsaIdMapping(reportNum, bsaId);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse FinCEN acknowledgment: {}", e.getMessage());
            result.setStatus("U"); // Unknown
            result.setErrorDescription("Parse error: " + e.getMessage());
        }
        return result;
    }

    private static String getTextContent(Document doc, String tagName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    private static String getAttributeValue(Document doc, String tagName, String attrName) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return ((org.w3c.dom.Element) nodes.item(0)).getAttribute(attrName);
        }
        return null;
    }

    private static String getChildText(org.w3c.dom.Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? nodes.item(0).getTextContent() : null;
    }

    public static class AcknowledgmentResult {
        private String status;
        private String errorCodes;
        private String errorDescription;
        private final Map<String, String> bsaIdMap = new HashMap<>();

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorCodes() { return errorCodes; }
        public void setErrorCodes(String errorCodes) { this.errorCodes = errorCodes; }
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

        public void addBsaIdMapping(String reportNumber, String bsaId) {
            bsaIdMap.put(reportNumber, bsaId);
        }

        public String getBsaIdForReport(String reportNumber) {
            return bsaIdMap.get(reportNumber);
        }
    }
}
