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
package com.fortify.cli.aviator.fpr.processor;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for common XML parsing operations.
 * Handles element text reading, section skipping, and event type conversions.
 */
public class XmlParserUtils {
    private static final Logger logger = LoggerFactory.getLogger(XmlParserUtils.class);

    /**
     * Read element text content safely.
     * Handles both simple text and CDATA content.
     */
    public static String readElementText(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder text = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                text.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return text.toString().trim();
    }

    /**
     * Read element content with markup (preserves inner XML structure).
     * Used for description fields that contain HTML/XML formatting.
     */
    public static String readElementContentWithMarkup(XMLStreamReader reader, String elementName) throws XMLStreamException {
        StringBuilder content = new StringBuilder();
        int depth = 1;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
                content.append("<").append(reader.getLocalName());
                for (int i = 0; i < reader.getAttributeCount(); i++) {
                    content.append(" ")
                            .append(reader.getAttributeLocalName(i))
                            .append("=\"")
                            .append(reader.getAttributeValue(i))
                            .append("\"");
                }
                content.append(">");
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if (depth > 0) {
                    content.append("</").append(reader.getLocalName()).append(">");
                }
            } else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                content.append(reader.getText());
            }
        }

        return content.toString().trim();
    }

    /**
     * Skip an entire XML section efficiently.
     * Maintains proper depth tracking to skip nested elements.
     */
    public static void skipSection(XMLStreamReader reader, String sectionName) throws XMLStreamException {
        String startElement = reader.getLocalName();
        int depth = 1;

        while (reader.hasNext() && depth > 0) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
                if (depth == 0 && startElement.equals(reader.getLocalName())) {
                    logger.trace("Skipped section: {}", sectionName);
                    return;
                }
            }
        }

        logger.warn("Reached end of stream while skipping section: {}", sectionName);
    }

    /**
     * Get readable event type name for debugging.
     */
    public static String getEventTypeName(int eventType) {
        switch (eventType) {
            case XMLStreamConstants.START_ELEMENT: return "START_ELEMENT";
            case XMLStreamConstants.END_ELEMENT: return "END_ELEMENT";
            case XMLStreamConstants.CHARACTERS: return "CHARACTERS";
            case XMLStreamConstants.CDATA: return "CDATA";
            case XMLStreamConstants.COMMENT: return "COMMENT";
            case XMLStreamConstants.SPACE: return "SPACE";
            case XMLStreamConstants.START_DOCUMENT: return "START_DOCUMENT";
            case XMLStreamConstants.END_DOCUMENT: return "END_DOCUMENT";
            case XMLStreamConstants.PROCESSING_INSTRUCTION: return "PROCESSING_INSTRUCTION";
            case XMLStreamConstants.ENTITY_REFERENCE: return "ENTITY_REFERENCE";
            case XMLStreamConstants.DTD: return "DTD";
            default: return "UNKNOWN(" + eventType + ")";
        }
    }

    /**
     * Safe integer parsing with null check.
     */
    public static Integer parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safe double parsing with default value.
     */
    public static double safeParseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Check if string is null or empty.
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Strip HTML/XML tags from text.
     */
    public static String stripTags(String text, boolean preserveWhitespace) {
        if (text == null) return "";
        String result = text.replaceAll("<[^>]+>", "");
        if (!preserveWhitespace) {
            result = result.replaceAll("\\s+", " ").trim();
        }
        return result;
    }
}
