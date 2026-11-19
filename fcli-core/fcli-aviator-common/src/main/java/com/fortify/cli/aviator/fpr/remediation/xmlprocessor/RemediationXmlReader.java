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
package com.fortify.cli.aviator.fpr.remediation.xmlprocessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.common.util.SecureXmlParserFactory;

/** Phase 1: parses {@code remediations.xml} into a DOM {@link Document}. No mapping, no business logic. */
public final class RemediationXmlReader {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationXmlReader.class);

    public Document read(Path remediationPath) {
        try (InputStream remediationStream = Files.newInputStream(remediationPath)) {
            var factory = SecureXmlParserFactory.newDocumentBuilderFactory(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(remediationStream);
        } catch (SAXParseException e) {
            LOG.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException(describeParseFailure(e), e);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException("Error processing remediation.xml file.", e);
        }
    }

    /** Parsing stays strict. A character that XML 1.0 forbids gets an explanation of what happened and what to do. */
    static String describeParseFailure(SAXParseException e) {
        String detail = e.getMessage() == null ? "" : e.getMessage();
        if (detail.contains("is an invalid XML character") || detail.contains("invalid XML character")) {
            return String.format("Error processing remediation.xml file: line %d, column %d: %s. The file contains a character "
                    + "that XML does not allow, usually a control character from source code or generated text, so it was "
                    + "generated incorrectly and cannot be used. Run the Aviator audit again with an fcli version that skips "
                    + "such remediations, or remove the character reference from the file.",
                    e.getLineNumber(), e.getColumnNumber(), detail);
        }
        return String.format("Error processing remediation.xml file: line %d, column %d: %s",
                e.getLineNumber(), e.getColumnNumber(), detail);
    }
}
