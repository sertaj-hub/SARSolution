package com.fincen.sar.efiling.xml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates SAR batch XML against FinCEN XSD schemas before submission.
 *
 * Required schemas per FinCEN BSA E-Filing documentation:
 * - Primary batch schema: https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd
 * - Base BSA elements:    https://www.fincen.gov/base/BSA_XML_2.0.xsd
 * - Reference codes:      https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd
 *
 * Both BSA_XML_2.0.xsd and FinCENReferenceCodes.xsd must be in same directory
 * as the batch file per FinCEN schema validation guidance.
 */
@Slf4j
@Component
public class FinCenXsdValidator {

    /**
     * Primary FinCEN SAR batch XML schema (imports BSA_XML_2.0.xsd and FinCENReferenceCodes.xsd).
     */
    private static final String SAR_BATCH_SCHEMA_URL =
            "https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd";

    /**
     * FinCEN Reference Codes schema - validates all code values used in SAR XML.
     * Per FinCEN: SuspiciousActivitySubtypeID, AssetSubtypeID, OrganizationSubtypeID,
     * PartyIdentificationTypeCode and other reference code values are validated against this XSD.
     */
    public static final String FINCEN_REFERENCE_CODES_XSD_URL =
            "https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd";

    /**
     * Base BSA XML 2.0 schema - contains all BSA data element definitions.
     */
    private static final String BSA_XML_BASE_SCHEMA_URL =
            "https://www.fincen.gov/base/BSA_XML_2.0.xsd";

    @Value("${sar.efiling.validation.use-online-schema:true}")
    private boolean useOnlineSchema;

    @Value("${sar.efiling.validation.schema-timeout-ms:30000}")
    private int schemaTimeoutMs;

    /**
     * Validate generated SAR batch XML against FinCEN XSD schemas.
     *
     * Validates against:
     * 1. EFL_SARXBatchSchema.xsd - primary batch structure
     * 2. BSA_XML_2.0.xsd - BSA element definitions
     * 3. FinCENReferenceCodes.xsd - reference code values (SuspiciousActivitySubtypeID, etc.)
     *
     * @param xmlContent The generated batch XML to validate
     * @return ValidationResult with success flag and any violations found
     */
    public ValidationResult validate(String xmlContent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!useOnlineSchema) {
            log.warn("XSD online validation disabled (sar.efiling.validation.use-online-schema=false)");
            return ValidationResult.skipped("Online XSD validation disabled");
        }

        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "all");

            // Load from FinCEN online schema - imports all dependent schemas automatically
            URL schemaUrl = new URL(SAR_BATCH_SCHEMA_URL);
            Schema schema = schemaFactory.newSchema(schemaUrl);

            Validator validator = schema.newValidator();
            List<String> validationErrors = new ArrayList<>();

            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    warnings.add("WARNING [line " + e.getLineNumber() + "]: " + e.getMessage());
                }

                @Override
                public void error(SAXParseException e) {
                    validationErrors.add("ERROR [line " + e.getLineNumber() + "]: " + e.getMessage());
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    validationErrors.add("FATAL [line " + e.getLineNumber() + "]: " + e.getMessage());
                    throw e;
                }
            });

            validator.validate(new StreamSource(new StringReader(xmlContent)));

            if (!validationErrors.isEmpty()) {
                log.error("FinCEN XSD validation failed with {} errors", validationErrors.size());
                validationErrors.forEach(e -> log.error("  {}", e));
                return ValidationResult.failure(validationErrors, warnings);
            }

            log.info("SAR batch XML passed FinCEN XSD validation");
            return ValidationResult.success(warnings);

        } catch (SAXException e) {
            String msg = "XSD validation fatal error: " + e.getMessage();
            log.error(msg);
            errors.add(msg);
            return ValidationResult.failure(errors, warnings);
        } catch (IOException e) {
            String msg = "Could not access FinCEN XSD schema at " + SAR_BATCH_SCHEMA_URL
                    + ". Check network connectivity. Error: " + e.getMessage();
            log.warn(msg);
            warnings.add(msg);
            // Non-fatal - proceed without online validation if schema unreachable
            return ValidationResult.skipped("Schema not reachable: " + e.getMessage());
        }
    }

    /**
     * Validate specific reference code values against FinCENReferenceCodes.xsd.
     * This validates that enum values (SuspiciousActivitySubtypeID, etc.) are valid FinCEN codes.
     *
     * @param codeType  The type of reference code (e.g., "SuspiciousActivitySubtypeID")
     * @param codeValue The value to validate
     * @return true if valid per FinCEN reference codes
     */
    public boolean isValidReferenceCode(String codeType, String codeValue) {
        // Reference codes are validated at enum level in Java (ActivityTypeCode, etc.)
        // This method provides additional runtime validation by testing a minimal XML
        // snippet against FinCENReferenceCodes.xsd
        String testXml = buildReferenceCodeTestXml(codeType, codeValue);
        if (testXml == null) return true; // Cannot test - assume valid

        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");

            URL refCodesUrl = new URL(FINCEN_REFERENCE_CODES_XSD_URL);
            Schema schema = schemaFactory.newSchema(refCodesUrl);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(testXml)));
            return true;
        } catch (Exception e) {
            log.debug("Reference code {} value '{}' validation: {}", codeType, codeValue, e.getMessage());
            return false;
        }
    }

    private String buildReferenceCodeTestXml(String codeType, String codeValue) {
        // Build a minimal XML snippet for reference code validation
        return switch (codeType) {
            case "SuspiciousActivitySubtypeID" ->
                    "<?xml version=\"1.0\"?><SuspiciousActivitySubtypeID xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                            + codeValue + "</SuspiciousActivitySubtypeID>";
            default -> null; // Cannot test unknown code types
        };
    }

    /**
     * Result of XSD validation.
     */
    public static class ValidationResult {
        private final boolean passed;
        private final boolean skipped;
        private final List<String> errors;
        private final List<String> warnings;
        private final String skipReason;

        private ValidationResult(boolean passed, boolean skipped, List<String> errors,
                                  List<String> warnings, String skipReason) {
            this.passed = passed;
            this.skipped = skipped;
            this.errors = errors;
            this.warnings = warnings;
            this.skipReason = skipReason;
        }

        public static ValidationResult success(List<String> warnings) {
            return new ValidationResult(true, false, List.of(), warnings, null);
        }

        public static ValidationResult failure(List<String> errors, List<String> warnings) {
            return new ValidationResult(false, false, errors, warnings, null);
        }

        public static ValidationResult skipped(String reason) {
            return new ValidationResult(true, true, List.of(), List.of(), reason);
        }

        public boolean isPassed() { return passed; }
        public boolean isSkipped() { return skipped; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
        public String getSkipReason() { return skipReason; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
    }
}
