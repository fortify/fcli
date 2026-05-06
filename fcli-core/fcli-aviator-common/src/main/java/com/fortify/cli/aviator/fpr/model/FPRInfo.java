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
package com.fortify.cli.aviator.fpr.model;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class FPRInfo {
    private String uuid;
    private String buildId;
    private String FPRName;
    private String sourceBasePath;
    private int numberOfFiles;
    private int scanTime;
    private FilterTemplate filterTemplate;
    private FilterSet defaultEnabledFilterSet;
    private String resultsTag;

    Logger logger = LoggerFactory.getLogger(FPRInfo.class);

    public FPRInfo(FprHandle fprHandle) {
        FPRName = String.valueOf(fprHandle.getFprPath().getFileName());
        buildId = "";
        try {
            extractInfoFromAuditFvdlStreaming(fprHandle);
        } catch (Exception e) {
            // It's better to wrap this in a specific runtime exception
            throw new RuntimeException("Failed to extract info from audit.fvdl", e);
        }
    }

    /**
     * Extract FPR metadata from audit.fvdl using streaming XML parsing (StAX).
     * More memory-efficient than DOM parsing for large files.
     *
     * Extracts:
     * - UUID
     * - Build information (BuildID, SourceBasePath, NumberFiles, ScanTime)
     *
     * @param fprHandle for getting path
     * @throws Exception if parsing fails
     */
    private void extractInfoFromAuditFvdlStreaming(FprHandle fprHandle) throws Exception {
        Path auditPath = fprHandle.getPath("/audit.fvdl");

        if (!Files.exists(auditPath)) {
            throw new IllegalStateException("audit.fvdl not found in FPR: " + fprHandle.getFprPath());
        }

        javax.xml.stream.XMLInputFactory factory = javax.xml.stream.XMLInputFactory.newInstance();
        // Security: Disable external entity processing
        factory.setProperty(javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(javax.xml.stream.XMLInputFactory.SUPPORT_DTD, false);

        try (java.io.InputStream inputStream = Files.newInputStream(auditPath)) {
            javax.xml.stream.XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

            boolean inBuild = false;
            String currentElement = null;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();

                    if ("UUID".equals(localName)) {
                        // Extract UUID text content
                        this.uuid = readElementText(reader);

                    } else if ("Build".equals(localName)) {
                        // Entering Build section
                        inBuild = true;

                    } else if (inBuild) {
                        // Inside Build section, capture element name
                        currentElement = localName;
                    }

                } else if (event == javax.xml.stream.XMLStreamConstants.CHARACTERS && inBuild && currentElement != null) {
                    // Read text content for Build child elements
                    String text = reader.getText().trim();
                    if (!text.isEmpty()) {
                        switch (currentElement) {
                            case "BuildID":
                                this.buildId = text;
                                break;
                            case "SourceBasePath":
                                this.sourceBasePath = text;
                                break;
                            case "NumberFiles":
                                this.numberOfFiles = parseIntegerContent(text);
                                break;
                            case "ScanTime":
                                this.scanTime = parseIntegerContent(text);
                                break;
                        }
                    }

                } else if (event == javax.xml.stream.XMLStreamConstants.END_ELEMENT) {
                    String localName = reader.getLocalName();

                    if ("Build".equals(localName)) {
                        // Exiting Build section, we have all needed data
                        inBuild = false;
                        // Early exit: we've extracted all needed metadata
                        if (this.uuid != null) {
                            break; // Stop parsing, we have everything
                        }

                    } else if (inBuild) {
                        // Clear current element when exiting child element
                        currentElement = null;
                    }
                }
            }

            reader.close();

        } catch (javax.xml.stream.XMLStreamException e) {
            throw new Exception("Failed to parse audit.fvdl using streaming parser", e);
        }

        if (buildId == null) {
            buildId = "";
        }
    }

    /**
     * Helper method to read element text content using StAX reader.
     * Advances reader to the text content and returns it.
     *
     * @param reader XMLStreamReader positioned at START_ELEMENT
     * @return Text content of the element, or empty string if no text
     * @throws javax.xml.stream.XMLStreamException if reading fails
     */
    private String readElementText(javax.xml.stream.XMLStreamReader reader) throws javax.xml.stream.XMLStreamException {
        StringBuilder text = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == javax.xml.stream.XMLStreamConstants.CHARACTERS) {
                text.append(reader.getText());
            } else if (event == javax.xml.stream.XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return text.toString().trim();
    }

    private void extractInfoFromAuditFvdl(FprHandle fprHandle) throws Exception {
        Path auditPath = fprHandle.getPath("/audit.fvdl");

        if (!Files.exists(auditPath)) {
            throw new IllegalStateException("audit.fvdl not found in FPR: " + fprHandle.getFprPath());
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document auditDoc;
        try (InputStream auditStream = Files.newInputStream(auditPath)) {
            auditDoc = builder.parse(auditStream);
        }

        NodeList uuidNodes = auditDoc.getElementsByTagName("UUID");
        if (uuidNodes.getLength() > 0) {
            this.uuid = uuidNodes.item(0).getTextContent();
        }

        NodeList buildNodes = auditDoc.getElementsByTagName("Build");
        if (buildNodes.getLength() > 0) {
            Element buildElement = (Element) buildNodes.item(0);
            this.buildId = getFirstElementContent(buildElement, "BuildID", "");
            this.sourceBasePath = getFirstElementContent(buildElement, "SourceBasePath", "");
            this.numberOfFiles = parseIntegerContent(getFirstElementContent(buildElement, "NumberFiles", "0"));
            this.scanTime = parseIntegerContent(getFirstElementContent(buildElement, "ScanTime", "0"));
        }
    }

    private String getFirstElementContent(Element parent, String tagName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0 && nodes.item(0) != null) {
            return nodes.item(0).getTextContent();
        }
        return defaultValue;
    }

    private int parseIntegerContent(String content) {

        if (StringUtil.isEmpty(content)) {
            return 0;
        }

        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            logger.warn("WARN: Error parsing integer: {}", content);
            return 0;
        }
    }

    public Optional<FilterSet> getDefaultEnabledFilterSet() {
        if (filterTemplate == null || filterTemplate.getFilterSets() == null) {
            return Optional.empty();
        }

        return filterTemplate.getFilterSets().stream()
                .filter(FilterSet::isEnabled)
                .findFirst();
    }
}
