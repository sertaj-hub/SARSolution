package com.fincen.sar.efiling.xml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates SAR batch XML against locally bundled FinCEN XSD schemas.
 *
 * Schemas bundled under src/main/resources/xsd/:
 *   EFL_SARXBatchSchema.xsd  (v1.2, 12/17/2021)
 *   BSA_XML_2.0.xsd           (v2.02)
 *   FinCENReferenceCodes.xsd  (v1.5)
 *   IC-ISM.xsd                (v13)
 */
@Slf4j
@Component
public class FinCenXsdValidator {

    public static final String FINCEN_REFERENCE_CODES_XSD_URL =
            "https://www.fincen.gov/system/files/schema/code/FinCENReferenceCodes.xsd";

    /** Maps known FinCEN remote schema URLs/namespaces to bundled classpath resources. */
    private static final Map<String, String> SCHEMA_MAP = new java.util.HashMap<>(Map.of(
        "https://www.fincen.gov/system/files/schema/base/EFL_SARXBatchSchema.xsd",        "/xsd/EFL_SARXBatchSchema.xsd",
        "https://www.fincen.gov/base/EFL_SARXBatchSchema.xsd",                            "/xsd/EFL_SARXBatchSchema.xsd",
        "https://www.fincen.gov/sites/default/files/schema/base/BSA_XML_2.0.xsd",         "/xsd/BSA_XML_2.0.xsd",
        "https://www.fincen.gov/base/BSA_XML_2.0.xsd",                                    "/xsd/BSA_XML_2.0.xsd",
        "https://www.fincen.gov/system/files/schema/base/Schema/ISM/IC-ISM.xsd",          "/xsd/IC-ISM.xsd",
        "https://www.fincen.gov/base/Schema/ISM/IC-ISM.xsd",                              "/xsd/IC-ISM.xsd",
        "urn:us:gov:ic:ism",                                                               "/xsd/IC-ISM.xsd",
        "https://www.fincen.gov/codes",                                                    "/xsd/FinCENReferenceCodes.xsd",
        FINCEN_REFERENCE_CODES_XSD_URL,                                                    "/xsd/FinCENReferenceCodes.xsd",
        "https://www.fincen.gov/system/files/schema/base/BSA_XML_2.0.xsd",                "/xsd/BSA_XML_2.0.xsd"
    ));

    private Schema cachedSchema;

    public ValidationResult validate(String xmlContent) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Phase 1: well-formedness (no network needed)
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.newDocumentBuilder().parse(new org.xml.sax.InputSource(new StringReader(xmlContent)));
        } catch (Exception e) {
            errors.add("XML is not well-formed: " + e.getMessage());
            return ValidationResult.failure(errors, warnings);
        }

        // Phase 2: XSD validation against bundled schemas
        try {
            Schema schema = getSchema();

            Validator validator = schema.newValidator();
            List<String> validationErrors = new ArrayList<>();

            validator.setErrorHandler(new org.xml.sax.ErrorHandler() {
                @Override public void warning(SAXParseException e) {
                    warnings.add("WARNING [line " + e.getLineNumber() + "]: " + e.getMessage());
                }
                @Override public void error(SAXParseException e) {
                    validationErrors.add("ERROR [line " + e.getLineNumber() + "]: " + e.getMessage());
                }
                @Override public void fatalError(SAXParseException e) throws SAXException {
                    validationErrors.add("FATAL [line " + e.getLineNumber() + "]: " + e.getMessage());
                    throw e;
                }
            });

            validator.validate(new StreamSource(new StringReader(xmlContent)));

            if (!validationErrors.isEmpty()) {
                log.error("FinCEN XSD validation failed with {} errors", validationErrors.size());
                return ValidationResult.failure(validationErrors, warnings);
            }

            log.info("SAR batch XML passed FinCEN XSD validation");
            return ValidationResult.success(warnings);

        } catch (SAXException | IOException e) {
            errors.add("Validation error: " + e.getMessage());
            return ValidationResult.failure(errors, warnings);
        }
    }

    /** Load and cache the compiled schema (thread-safe lazy init). */
    private synchronized Schema getSchema() throws SAXException, IOException {
        if (cachedSchema != null) return cachedSchema;

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
        factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setResourceResolver(new ClasspathResourceResolver());

        InputStream primary = getClass().getResourceAsStream("/xsd/EFL_SARXBatchSchema.xsd");
        if (primary == null) throw new IOException("Bundled EFL_SARXBatchSchema.xsd not found on classpath");

        cachedSchema = factory.newSchema(new StreamSource(primary, "https://www.fincen.gov/system/files/schema/base/EFL_SARXBatchSchema.xsd"));
        log.info("FinCEN XSD schemas loaded from classpath");
        return cachedSchema;
    }

    /** Resolves remote FinCEN schema URLs to locally bundled classpath resources. */
    private static class ClasspathResourceResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                                       String systemId, String baseURI) {
            String localPath = SCHEMA_MAP.get(systemId);
            if (localPath == null && namespaceURI != null) {
                localPath = SCHEMA_MAP.get(namespaceURI);
            }
            if (localPath == null) {
                log.debug("No local mapping for schema: systemId={} ns={}", systemId, namespaceURI);
                return null;
            }

            InputStream stream = getClass().getResourceAsStream(localPath);
            if (stream == null) {
                log.warn("Bundled schema not found: {}", localPath);
                return null;
            }

            log.debug("Resolved {} → classpath:{}", systemId, localPath);
            return new ClasspathLSInput(stream, systemId);
        }
    }

    private static class ClasspathLSInput implements LSInput {
        private final InputStream stream;
        private final String systemId;

        ClasspathLSInput(InputStream stream, String systemId) {
            this.stream = stream;
            this.systemId = systemId;
        }

        @Override public InputStream getByteStream() { return stream; }
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String s) {}
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String s) {}
        @Override public Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(Reader r) {}
        @Override public void setByteStream(InputStream i) {}
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String s) {}
        @Override public String getPublicId() { return null; }
        @Override public void setPublicId(String s) {}
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean b) {}
        @Override public String getEncoding() { return "UTF-8"; }
        @Override public void setEncoding(String s) {}
    }

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
