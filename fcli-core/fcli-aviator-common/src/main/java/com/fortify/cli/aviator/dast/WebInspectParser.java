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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator.util.FprHandle;

/**
 * Parser for WebInspect XML files contained in DAST FPR files.
 * Uses DOM-based parsing for simplicity (as specified for PoC).
 */
public class WebInspectParser {
    private static final Logger logger = LoggerFactory.getLogger(WebInspectParser.class);

    private final FprHandle fprHandle;

    public WebInspectParser(FprHandle fprHandle) {
        this.fprHandle = fprHandle;
    }

    /**
     * Parses the webinspect.xml file and returns all DAST issues.
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
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Get all Session elements
            NodeList sessionNodes = document.getElementsByTagName("Session");
            logger.debug("Found {} sessions in webinspect.xml", sessionNodes.getLength());

            for (int i = 0; i < sessionNodes.getLength(); i++) {
                Element sessionElement = (Element) sessionNodes.item(i);
                String sessionUrl = getElementText(sessionElement, "URL");

                // Get Issues element within this session
                NodeList issuesNodes = sessionElement.getElementsByTagName("Issues");
                if (issuesNodes.getLength() > 0) {
                    Element issuesElement = (Element) issuesNodes.item(0);
                    NodeList issueNodes = issuesElement.getElementsByTagName("Issue");

                    for (int j = 0; j < issueNodes.getLength(); j++) {
                        Element issueElement = (Element) issueNodes.item(j);
                        DastIssue issue = parseIssue(issueElement, sessionUrl);
                        if (issue != null) {
                            issues.add(issue);
                        }
                    }
                }
            }

            logger.info("Parsed {} DAST issues from webinspect.xml", issues.size());

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to parse webinspect.xml: " + e.getMessage(), e);
        }

        return issues;
    }

    /**
     * Parses the webinspect.xml file and returns all DAST sessions with their issues.
     * Includes Base64-decoded raw request/response data for audit purposes.
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
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Get all Session elements
            NodeList sessionNodes = document.getElementsByTagName("Session");
            logger.debug("Found {} sessions in webinspect.xml", sessionNodes.getLength());

            for (int i = 0; i < sessionNodes.getLength(); i++) {
                Element sessionElement = (Element) sessionNodes.item(i);
                DastSession session = parseSession(sessionElement);

                // Only include sessions that have issues
                if (session.hasIssues()) {
                    sessions.add(session);
                }
            }

            int totalIssues = sessions.stream().mapToInt(DastSession::getIssueCount).sum();
            logger.info("Parsed {} sessions with {} total DAST issues from webinspect.xml",
                sessions.size(), totalIssues);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Failed to parse webinspect.xml: " + e.getMessage(), e);
        }

        return sessions;
    }

    private DastSession parseSession(Element sessionElement) {
        DastSession session = new DastSession();

        session.setRequestId(sessionElement.getAttribute("requestId"));
        session.setUrl(getElementText(sessionElement, "URL"));
        session.setScheme(getElementText(sessionElement, "Scheme"));
        session.setHost(getElementText(sessionElement, "Host"));
        session.setAttackParamDescriptor(getElementText(sessionElement, "AttackParamDescriptor"));

        // Parse port
        String portStr = getElementText(sessionElement, "Port");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                session.setPort(Integer.parseInt(portStr));
            } catch (NumberFormatException e) {
                session.setPort(0);
            }
        }

        // Decode raw request (Base64)
        NodeList rawRequestNodes = sessionElement.getElementsByTagName("RawRequest");
        if (rawRequestNodes.getLength() > 0) {
            Element rawRequestElement = (Element) rawRequestNodes.item(0);
            String base64Content = rawRequestElement.getTextContent();
            if (base64Content != null && !base64Content.trim().isEmpty()) {
                session.setRawRequest(decodeBase64(base64Content.trim()));
            }
        }

        // Decode raw response (Base64)
        NodeList rawResponseNodes = sessionElement.getElementsByTagName("RawResponse");
        if (rawResponseNodes.getLength() > 0) {
            Element rawResponseElement = (Element) rawResponseNodes.item(0);
            String base64Content = rawResponseElement.getTextContent();
            if (base64Content != null && !base64Content.trim().isEmpty()) {
                session.setRawResponse(decodeBase64(base64Content.trim()));
            }
        }

        // Parse issues within this session
        NodeList issuesNodes = sessionElement.getElementsByTagName("Issues");
        if (issuesNodes.getLength() > 0) {
            Element issuesElement = (Element) issuesNodes.item(0);
            NodeList issueNodes = issuesElement.getElementsByTagName("Issue");

            for (int j = 0; j < issueNodes.getLength(); j++) {
                Element issueElement = (Element) issueNodes.item(j);
                DastIssue issue = parseIssueForAudit(issueElement, session.getUrl());
                if (issue != null) {
                    session.getIssues().add(issue);
                }
            }
        }

        return session;
    }

    /**
     * Parse issue with all ReportSections for audit purposes.
     */
    private DastIssue parseIssueForAudit(Element issueElement, String sessionUrl) {
        DastIssue issue = new DastIssue();

        issue.setId(issueElement.getAttribute("id"));
        issue.setCheckTypeId(getElementText(issueElement, "CheckTypeID"));
        issue.setEngineType(getElementText(issueElement, "EngineType"));
        issue.setVulnerabilityId(getElementText(issueElement, "VulnerabilityID"));
        issue.setName(getElementText(issueElement, "Name"));
        issue.setSessionUrl(sessionUrl);

        // Parse severity
        String severityStr = getElementText(issueElement, "Severity");
        if (severityStr != null && !severityStr.isEmpty()) {
            try {
                issue.setSeverity(Integer.parseInt(severityStr));
            } catch (NumberFormatException e) {
                issue.setSeverity(0);
            }
        }

        // Parse all classifications
        NodeList classificationsNodes = issueElement.getElementsByTagName("Classifications");
        if (classificationsNodes.getLength() > 0) {
            Element classificationsElement = (Element) classificationsNodes.item(0);
            NodeList classificationNodes = classificationsElement.getElementsByTagName("Classification");

            for (int i = 0; i < classificationNodes.getLength(); i++) {
                Element classificationElement = (Element) classificationNodes.item(i);
                String kind = classificationElement.getAttribute("kind");
                String value = classificationElement.getTextContent().trim();

                issue.getClassifications().put(kind, value);

                if ("7PK Category".equals(kind)) {
                    issue.setCategory(value);
                } else if ("CWE".equals(kind)) {
                    issue.setCweId(classificationElement.getAttribute("identifier"));
                    issue.setCweDescription(value);
                }
            }
        }

        // Fallback for FoD DAST format: use Name element as category
        if ((issue.getCategory() == null || issue.getCategory().isEmpty()) && issue.getName() != null) {
            issue.setCategory(issue.getName().trim());
        }

        // Parse repro step URLs
        NodeList reproStepsNodes = issueElement.getElementsByTagName("ReproSteps");
        if (reproStepsNodes.getLength() > 0) {
            Element reproStepsElement = (Element) reproStepsNodes.item(0);
            NodeList reproStepNodes = reproStepsElement.getElementsByTagName("ReproStep");

            for (int i = 0; i < reproStepNodes.getLength(); i++) {
                Element reproStepElement = (Element) reproStepNodes.item(i);
                String url = getElementText(reproStepElement, "Url");
                if (url != null && !url.isEmpty()) {
                    issue.getReproStepUrls().add(url);
                }
            }
        }

        // Parse ALL ReportSections
        NodeList reportSectionNodes = issueElement.getElementsByTagName("ReportSection");
        for (int i = 0; i < reportSectionNodes.getLength(); i++) {
            Element reportSectionElement = (Element) reportSectionNodes.item(i);
            String sectionName = getElementText(reportSectionElement, "Name");
            String sectionText = getElementText(reportSectionElement, "SectionText");

            if (sectionText != null && !sectionText.isEmpty()) {
                String cleanText = stripHtmlTags(sectionText);
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
                }
            }
        }

        parseExternalFindings(issueElement, issue);

        return issue;
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

    private DastIssue parseIssue(Element issueElement, String sessionUrl) {
        DastIssue issue = new DastIssue();

        issue.setId(issueElement.getAttribute("id"));
        issue.setCheckTypeId(getElementText(issueElement, "CheckTypeID"));
        issue.setEngineType(getElementText(issueElement, "EngineType"));
        issue.setVulnerabilityId(getElementText(issueElement, "VulnerabilityID"));
        issue.setName(getElementText(issueElement, "Name"));
        issue.setSessionUrl(sessionUrl);

        // Parse severity
        String severityStr = getElementText(issueElement, "Severity");
        if (severityStr != null && !severityStr.isEmpty()) {
            try {
                issue.setSeverity(Integer.parseInt(severityStr));
            } catch (NumberFormatException e) {
                issue.setSeverity(0);
            }
        }

        // Parse classifications to get category and CWE (standard SSC format)
        NodeList classificationsNodes = issueElement.getElementsByTagName("Classifications");
        if (classificationsNodes.getLength() > 0) {
            Element classificationsElement = (Element) classificationsNodes.item(0);
            NodeList classificationNodes = classificationsElement.getElementsByTagName("Classification");

            for (int i = 0; i < classificationNodes.getLength(); i++) {
                Element classificationElement = (Element) classificationNodes.item(i);
                String kind = classificationElement.getAttribute("kind");

                if ("7PK Category".equals(kind)) {
                    issue.setCategory(classificationElement.getTextContent().trim());
                } else if ("CWE".equals(kind)) {
                    issue.setCweId(classificationElement.getAttribute("identifier"));
                }
            }
        }

        // Fallback for FoD DAST format: use Name element as category
        // The full name (e.g., "Privacy Violation: Autocomplete") is the category,
        // matching how SAST categories are formatted as "Type: SubType"
        if ((issue.getCategory() == null || issue.getCategory().isEmpty()) && issue.getName() != null) {
            issue.setCategory(issue.getName().trim());
        }

        // Parse repro step URLs
        NodeList reproStepsNodes = issueElement.getElementsByTagName("ReproSteps");
        if (reproStepsNodes.getLength() > 0) {
            Element reproStepsElement = (Element) reproStepsNodes.item(0);
            NodeList reproStepNodes = reproStepsElement.getElementsByTagName("ReproStep");

            for (int i = 0; i < reproStepNodes.getLength(); i++) {
                Element reproStepElement = (Element) reproStepNodes.item(i);
                String url = getElementText(reproStepElement, "Url");
                if (url != null && !url.isEmpty()) {
                    issue.getReproStepUrls().add(url);
                }
            }
        }

        // Parse ReportSection to get Summary
        NodeList reportSectionNodes = issueElement.getElementsByTagName("ReportSection");
        for (int i = 0; i < reportSectionNodes.getLength(); i++) {
            Element reportSectionElement = (Element) reportSectionNodes.item(i);
            String sectionName = getElementText(reportSectionElement, "Name");
            if ("Summary".equals(sectionName)) {
                String sectionText = getElementText(reportSectionElement, "SectionText");
                if (sectionText != null && !sectionText.isEmpty()) {
                    // Strip HTML tags for cleaner summary
                    issue.setSummary(stripHtmlTags(sectionText));
                }
                break;
            }
        }

        parseExternalFindings(issueElement, issue);

        return issue;
    }

    /**
     * Reads {@code <ExternalFindings>/<ExternalFinding>/<OriginFindingID>} from the given
     * issue element and populates {@link DastIssue#getExistingCorrelatedSastIds()}.
     * This records which SAST findings were already correlated in a prior run so that
     * the processor can skip re-submitting them to the gRPC service.
     */
    private void parseExternalFindings(Element issueElement, DastIssue issue) {
        NodeList efContainers = issueElement.getElementsByTagName("ExternalFindings");
        if (efContainers.getLength() == 0) return;

        Element efContainer = (Element) efContainers.item(0);
        NodeList efNodes = efContainer.getElementsByTagName("ExternalFinding");
        for (int i = 0; i < efNodes.getLength(); i++) {
            Element ef = (Element) efNodes.item(i);
            String originFindingId = getElementText(ef, "OriginFindingID");
            if (originFindingId != null && !originFindingId.isEmpty()) {
                issue.getExistingCorrelatedSastIds().add(originFindingId);
            }
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    private String stripHtmlTags(String html) {
        if (html == null) {
            return null;
        }
        // Remove HTML tags and normalize whitespace
        return html.replaceAll("<[^>]+>", " ")
                   .replaceAll("\\s+", " ")
                   .trim();
    }

    private DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }
}
