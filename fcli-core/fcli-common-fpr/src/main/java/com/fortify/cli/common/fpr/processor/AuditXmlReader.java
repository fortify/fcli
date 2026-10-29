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
package com.fortify.cli.common.fpr.processor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.fpr.model.AuditIssue;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.util.SecureXmlParserFactory;

/**
 * Reads {@code audit.xml} from an FPR into {@link AuditIssue} objects. If the FPR has no
 * {@code audit.xml}, an empty default audit document is created so that callers always get a
 * usable DOM to work with.
 */
public final class AuditXmlReader {
    public static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";

    private static final Logger logger = LoggerFactory.getLogger(AuditXmlReader.class);

    /** The parsed audit DOM together with the audit issues found in it, keyed by instance id. */
    public record AuditXmlContent(Document document, Map<String, AuditIssue> auditIssues) {}

    public AuditXmlContent read(FprHandle fprHandle) {
        Path auditPath = fprHandle.getPath("/audit.xml");
        Map<String, AuditIssue> auditIssueMap = new HashMap<>();
        Document auditDoc;

        try {
            if (!Files.exists(auditPath)) {
                logger.debug("audit.xml not found. Creating a default audit.xml.");
                auditDoc = createDefaultAuditXml();
            } else {
                var factory = SecureXmlParserFactory.newDocumentBuilderFactory(true);
                DocumentBuilder builder = factory.newDocumentBuilder();

                try (InputStream auditStream = Files.newInputStream(auditPath)) {
                    auditDoc = builder.parse(auditStream);
                }
            }
            NodeList issueNodes = auditDoc.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Issue");
            for (int i = 0; i < issueNodes.getLength(); i++) {
                Element issueElement = (Element) issueNodes.item(i);
                AuditIssue auditIssue = processAuditIssue(issueElement);
                auditIssueMap.put(auditIssue.getInstanceId(), auditIssue);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.error("Error parsing audit.xml file: {}", auditPath, e);
            throw new FcliTechnicalException("Error processing audit.xml file.", e);
        } catch (FcliTechnicalException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error processing audit.xml: {}", auditPath, e);
            throw new FcliTechnicalException("Unexpected error processing audit.xml.", e);
        }

        return new AuditXmlContent(auditDoc, auditIssueMap);
    }

    private Document createDefaultAuditXml() throws FcliTechnicalException {
        try {
            var docFactory = SecureXmlParserFactory.newDocumentBuilderFactory(true);

            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:Audit");
            doc.appendChild(rootElement);
            rootElement.setPrefix("ns2");

            rootElement.setAttribute("xmlns:ns3", "xmlns://www.fortifysoftware.com/schema/activitytemplate");
            rootElement.setAttribute("xmlns:ns4", "xmlns://www.fortifysoftware.com/schema/wsTypes");
            rootElement.setAttribute("xmlns:ns5", "xmlns://www.fortify.com/schema/issuemanagement");
            rootElement.setAttribute("xmlns:ns6", "http://www.fortify.com/schema/fws");
            rootElement.setAttribute("xmlns:ns7", "xmlns://www.fortifysoftware.com/schema/runtime");
            rootElement.setAttribute("xmlns:ns8", "xmlns://www.fortifysoftware.com/schema/seed");
            rootElement.setAttribute("xmlns:ns9", "xmlns://www.fortify.com/schema/attachments");
            rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rootElement.setAttribute("version", "4.4");

            Element projectInfoElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:ProjectInfo");
            Element projectNameElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:Name");
            projectNameElement.setTextContent("Unknown Project");
            Element projectVersionIdElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:ProjectVersionId");
            projectVersionIdElement.setTextContent("-1");
            Element writeDateElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:WriteDate");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            writeDateElement.setTextContent(dateFormat.format(new Date()));

            projectInfoElement.appendChild(projectNameElement);
            projectInfoElement.appendChild(projectVersionIdElement);
            projectInfoElement.appendChild(writeDateElement);
            rootElement.appendChild(projectInfoElement);

            Element issueListElement = doc.createElementNS(AUDIT_NAMESPACE_URI, "ns2:IssueList");
            rootElement.appendChild(issueListElement);

            return doc;

        } catch (ParserConfigurationException e) {
            throw new FcliTechnicalException("Failed to create default audit.xml document.", e);
        }
    }

    private AuditIssue processAuditIssue(Element issueElement) {
        AuditIssue.AuditIssueBuilder auditIssueBuilder = AuditIssue.builder();

        auditIssueBuilder.instanceId(issueElement.getAttribute("instanceId"));
        auditIssueBuilder.suppressed(Boolean.parseBoolean(issueElement.getAttribute("suppressed")));

        String revisionStr = issueElement.getAttribute("revision");
        auditIssueBuilder.revision(Optional.of(revisionStr).filter(str -> !str.isEmpty()).map(Integer::parseInt).orElse(0));

        Map<String, String> tags = new HashMap<>();
        NodeList tagNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Tag");
        for (int j = 0; j < tagNodes.getLength(); j++) {
            Element tagElement = (Element) tagNodes.item(j);
            String tagId = tagElement.getAttribute("id");
            String tagValue = Optional.ofNullable(getTagValue(tagElement)).orElse("");
            tags.put(tagId, tagValue);
        }
        auditIssueBuilder.tags(tags);
        auditIssueBuilder.lastTagUsernames(lastTagUsernames(issueElement));

        List<AuditIssue.Comment> threadedComments = new ArrayList<>();
        NodeList commentNodes = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Comment");
        for (int j = 0; j < commentNodes.getLength(); j++) {
            Element commentElement = (Element) commentNodes.item(j);
            AuditIssue.Comment comment = AuditIssue.Comment.builder()
                    .content(Optional.ofNullable(getFirstElementContentNS(commentElement, "Content")).orElse(""))
                    .username(Optional.ofNullable(getFirstElementContentNS(commentElement, "Username")).orElse(""))
                    .timestamp(Optional.ofNullable(getFirstElementContentNS(commentElement, "Timestamp")).orElse(""))
                    .build();
            threadedComments.add(comment);
        }
        auditIssueBuilder.threadedComments(threadedComments);

        return auditIssueBuilder.build();
    }

    private Map<String, String> lastTagUsernames(Element issueElement) {
        Map<String, String> lastTagUsernames = new HashMap<>();
        NodeList tagHistories = issueElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "TagHistory");
        for (int index = 0; index < tagHistories.getLength(); index++) {
            Element tagHistory = (Element) tagHistories.item(index);
            NodeList tags = tagHistory.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Tag");
            if (tags.getLength() == 0) {
                continue;
            }
            String tagId = ((Element) tags.item(0)).getAttribute("id");
            String username = Optional.ofNullable(getFirstElementContentNS(tagHistory, "Username")).orElse("");
            if (tagId != null && !tagId.isBlank()) {
                lastTagUsernames.put(tagId, username);
            }
        }
        return lastTagUsernames;
    }

    private String getTagValue(Element tagElement) {
        NodeList valueNodes = tagElement.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, "Value");
        if (valueNodes.getLength() > 0) {
            return valueNodes.item(0).getTextContent();
        }
        return "";
    }

    private String getFirstElementContentNS(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagNameNS(AUDIT_NAMESPACE_URI, tagName);
        if (nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
}
