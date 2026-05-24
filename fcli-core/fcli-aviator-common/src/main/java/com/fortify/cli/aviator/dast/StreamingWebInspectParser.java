/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.aviator.dast;

import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.readElementText;
import static com.fortify.cli.aviator.fpr.processor.XmlParserUtils.skipSection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.util.FprHandle;

/**
 * Streaming (StAX-based) parser for WebInspect XML files contained in DAST FPR files.
 * This is the memory-efficient alternative to the DOM-based {@link WebInspectParser}.
 * Uses the same streaming pattern as {@link com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor}.
 */
public class StreamingWebInspectParser {
    private static final Logger logger = LoggerFactory.getLogger(StreamingWebInspectParser.class);

    private final FprHandle fprHandle;
    private final XMLInputFactory xmlInputFactory;

    public StreamingWebInspectParser(FprHandle fprHandle) {
        this.fprHandle = fprHandle;
        this.xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    /**
     * Parses the webinspect.xml file and returns all DAST issues.
     * Equivalent to {@link WebInspectParser#parse()} but uses streaming (StAX) parsing
     * for lower memory consumption on large scan results.
     *
     * @return List of DastIssue objects
     */
    public List<DastIssue> parse() {
        List<DastIssue> issues = new ArrayList<>();
        Path webInspectPath = fprHandle.getPath("/webinspect.xml");

        if (!Files.exists(webInspectPath)) {
            throw new RuntimeException("webinspect.xml not found in DAST FPR");
        }

        try (InputStream inputStream = Files.newInputStream(webInspectPath)) {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);

            try {
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT
                            && "Session".equals(reader.getLocalName())) {
                        parseSessionForIssues(reader, issues);
                    }
                }
            } finally {
                reader.close();
            }

            logger.info("Parsed {} DAST issues from webinspect.xml (streaming)", issues.size());

        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to parse webinspect.xml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read webinspect.xml: " + e.getMessage(), e);
        }

        return issues;
    }

    /**
     * Parses the webinspect.xml file and returns all DAST sessions with their issues.
     * Includes Base64-decoded raw request/response data for audit purposes.
     * Equivalent to {@link WebInspectParser#parseSessions()} but uses streaming parsing.
     *
     * @return List of DastSession objects with issues
     */
    public List<DastSession> parseSessions() {
        List<DastSession> sessions = new ArrayList<>();
        Path webInspectPath = fprHandle.getPath("/webinspect.xml");

        if (!Files.exists(webInspectPath)) {
            throw new RuntimeException("webinspect.xml not found in DAST FPR");
        }

        try (InputStream inputStream = Files.newInputStream(webInspectPath)) {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(inputStream);

            try {
                while (reader.hasNext()) {
                    int event = reader.next();

                    if (event == XMLStreamConstants.START_ELEMENT
                            && "Session".equals(reader.getLocalName())) {
                        DastSession session = parseFullSession(reader);
                        if (session.hasIssues()) {
                            sessions.add(session);
                        }
                    }
                }
            } finally {
                reader.close();
            }

            int totalIssues = sessions.stream().mapToInt(DastSession::getIssueCount).sum();
            logger.info("Parsed {} sessions with {} total DAST issues from webinspect.xml (streaming)",
                    sessions.size(), totalIssues);

        } catch (XMLStreamException e) {
            throw new RuntimeException("Failed to parse webinspect.xml: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read webinspect.xml: " + e.getMessage(), e);
        }

        return sessions;
    }

    // =========================================================================
    // Session parsing for parse() — collects issues only, skips raw request/response
    // =========================================================================

    private void parseSessionForIssues(XMLStreamReader reader, List<DastIssue> issues)
            throws XMLStreamException {

        String sessionUrl = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "URL":
                        sessionUrl = readElementText(reader);
                        break;
                    case "Issue":
                        DastIssue issue = parseIssue(reader, sessionUrl);
                        if (issue != null) {
                            issues.add(issue);
                        }
                        break;
                    case "RawRequest":
                    case "RawResponse":
                    case "Request":
                    case "Response":
                        skipSection(reader, localName);
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Session".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    // =========================================================================
    // Session parsing for parseSessions() — full session including raw data
    // =========================================================================

    private DastSession parseFullSession(XMLStreamReader reader) throws XMLStreamException {
        var session = new DastSession();
        session.setRequestId(reader.getAttributeValue(null, "requestId"));

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "URL":
                        session.setUrl(readElementText(reader));
                        break;
                    case "Scheme":
                        session.setScheme(readElementText(reader));
                        break;
                    case "Host":
                        session.setHost(readElementText(reader));
                        break;
                    case "Port":
                        session.setPort(parseIntSafe(readElementText(reader), 0));
                        break;
                    case "AttackParamDescriptor":
                        session.setAttackParamDescriptor(readElementText(reader));
                        break;
                    case "RawRequest":
                        session.setRawRequest(decodeBase64(readElementText(reader)));
                        break;
                    case "RawResponse":
                        session.setRawResponse(decodeBase64(readElementText(reader)));
                        break;
                    case "Issue":
                        DastIssue issue = parseIssueForAudit(reader, session.getUrl());
                        if (issue != null) {
                            session.getIssues().add(issue);
                        }
                        break;
                    case "Request":
                    case "Response":
                        skipSection(reader, localName);
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Session".equals(reader.getLocalName())) {
                return session;
            }
        }

        return session;
    }

    // =========================================================================
    // Issue parsing — lightweight version for parse()
    // =========================================================================

    private DastIssue parseIssue(XMLStreamReader reader, String sessionUrl) throws XMLStreamException {
        var issue = new DastIssue();
        issue.setId(reader.getAttributeValue(null, "id"));
        issue.setSessionUrl(sessionUrl);

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "CheckTypeID":
                        issue.setCheckTypeId(readElementText(reader));
                        break;
                    case "EngineType":
                        issue.setEngineType(readElementText(reader));
                        break;
                    case "VulnerabilityID":
                        issue.setVulnerabilityId(readElementText(reader));
                        break;
                    case "Severity":
                        issue.setSeverity(parseIntSafe(readElementText(reader), 0));
                        break;
                    case "Name":
                        issue.setName(readElementText(reader));
                        break;
                    case "Classifications":
                        parseClassificationsBasic(reader, issue);
                        break;
                    case "ReproSteps":
                        parseReproSteps(reader, issue);
                        break;
                    case "ReportSection":
                        parseReportSectionSummaryOnly(reader, issue);
                        break;
                    case "ExternalFindings":
                        parseExternalFindings(reader, issue);
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Issue".equals(reader.getLocalName())) {
                break;
            }
        }

        return issue;
    }

    // =========================================================================
    // Issue parsing — full version for parseSessions() / audit
    // =========================================================================

    private DastIssue parseIssueForAudit(XMLStreamReader reader, String sessionUrl) throws XMLStreamException {
        var issue = new DastIssue();
        issue.setId(reader.getAttributeValue(null, "id"));
        issue.setSessionUrl(sessionUrl);

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                switch (localName) {
                    case "CheckTypeID":
                        issue.setCheckTypeId(readElementText(reader));
                        break;
                    case "EngineType":
                        issue.setEngineType(readElementText(reader));
                        break;
                    case "VulnerabilityID":
                        issue.setVulnerabilityId(readElementText(reader));
                        break;
                    case "Severity":
                        issue.setSeverity(parseIntSafe(readElementText(reader), 0));
                        break;
                    case "Name":
                        issue.setName(readElementText(reader));
                        break;
                    case "Classifications":
                        parseClassificationsFull(reader, issue);
                        break;
                    case "ReproSteps":
                        parseReproSteps(reader, issue);
                        break;
                    case "ReportSection":
                        parseReportSectionAll(reader, issue);
                        break;
                    case "ExternalFindings":
                        parseExternalFindings(reader, issue);
                        break;
                    default:
                        break;
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Issue".equals(reader.getLocalName())) {
                break;
            }
        }

        applyFallbackCategory(issue);
        return issue;
    }

    // =========================================================================
    // Classifications parsing
    // =========================================================================

    /**
     * Parse Classifications for the lightweight parse() flow.
     * Only extracts "7PK Category" and "CWE" kinds.
     */
    private void parseClassificationsBasic(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT
                    && "Classification".equals(reader.getLocalName())) {

                String kind = reader.getAttributeValue(null, "kind");
                String identifier = reader.getAttributeValue(null, "identifier");
                String text = readElementText(reader);

                if ("7PK Category".equals(kind)) {
                    issue.setCategory(text.trim());
                } else if ("CWE".equals(kind)) {
                    issue.setCweId(identifier);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Classifications".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    /**
     * Parse Classifications for the full audit flow.
     * Stores all classification kinds in the classifications map.
     */
    private void parseClassificationsFull(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT
                    && "Classification".equals(reader.getLocalName())) {

                String kind = reader.getAttributeValue(null, "kind");
                String identifier = reader.getAttributeValue(null, "identifier");
                String text = readElementText(reader);
                String trimmedText = text != null ? text.trim() : "";

                issue.getClassifications().put(kind, trimmedText);

                if ("7PK Category".equals(kind)) {
                    issue.setCategory(trimmedText);
                } else if ("CWE".equals(kind)) {
                    issue.setCweId(identifier);
                    issue.setCweDescription(trimmedText);
                }

            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "Classifications".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    // =========================================================================
    // ReproSteps parsing
    // =========================================================================

    private void parseReproSteps(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("Url".equals(reader.getLocalName())) {
                    String url = readElementText(reader);
                    if (url != null && !url.isEmpty()) {
                        issue.getReproStepUrls().add(url);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "ReproSteps".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    // =========================================================================
    // ReportSection parsing
    // =========================================================================

    /**
     * Parse a single ReportSection — only captures the "Summary" section (for parse()).
     */
    private void parseReportSectionSummaryOnly(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        String sectionName = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Name".equals(localName)) {
                    sectionName = readElementText(reader);
                } else if ("SectionText".equals(localName)) {
                    if ("Summary".equals(sectionName)) {
                        String text = readElementText(reader);
                        if (text != null && !text.isEmpty()) {
                            issue.setSummary(stripHtmlTags(text));
                        }
                    } else {
                        // Skip non-Summary section text
                        readElementText(reader);
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "ReportSection".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    /**
     * Parse a single ReportSection — captures all known section types (for parseSessions()/audit).
     */
    private void parseReportSectionAll(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        String sectionName = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = reader.getLocalName();

                if ("Name".equals(localName)) {
                    sectionName = readElementText(reader);
                } else if ("SectionText".equals(localName)) {
                    String text = readElementText(reader);
                    if (text != null && !text.isEmpty() && sectionName != null) {
                        String cleanText = stripHtmlTags(text);
                        switch (sectionName) {
                            case "Summary":
                                issue.setSummary(cleanText);
                                break;
                            case "Implication":
                                issue.setImplication(cleanText);
                                break;
                            case "Execution":
                                issue.setExecution(cleanText);
                                break;
                            case "Fix":
                                issue.setFix(cleanText);
                                break;
                            case "Reference Info":
                                issue.setReferenceInfo(cleanText);
                                break;
                            default:
                                break;
                        }
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "ReportSection".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Parses {@code <ExternalFindings>} and populates
     * {@link DastIssue#getExistingCorrelatedSastIds()} with every
     * {@code <OriginFindingID>} found inside. These IDs represent SAST
     * findings already correlated to this DAST issue in a prior run, and
     * are used to skip redundant gRPC calls.
     */
    private void parseExternalFindings(XMLStreamReader reader, DastIssue issue)
            throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT
                    && "OriginFindingID".equals(reader.getLocalName())) {
                String originFindingId = readElementText(reader);
                if (originFindingId != null && !originFindingId.isEmpty()) {
                    issue.getExistingCorrelatedSastIds().add(originFindingId);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && "ExternalFindings".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private void applyFallbackCategory(DastIssue issue) {
        if ((issue.getCategory() == null || issue.getCategory().isEmpty())
                && issue.getName() != null) {
            issue.setCategory(issue.getName().trim());
        }
    }

    private String decodeBase64(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to decode Base64 content: {}", e.getMessage());
            return null;
        }
    }

    private String stripHtmlTags(String html) {
        if (html == null) {
            return null;
        }
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int parseIntSafe(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
