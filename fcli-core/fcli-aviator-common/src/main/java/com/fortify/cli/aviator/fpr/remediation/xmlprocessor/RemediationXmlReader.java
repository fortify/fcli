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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;

/** Phase 1: parses {@code remediations.xml} into a DOM {@link Document}. No mapping, no business logic. */
public final class RemediationXmlReader {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationXmlReader.class);

    public Document read(Path remediationPath) {
        try (InputStream remediationStream = Files.newInputStream(remediationPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(remediationStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOG.error("Error parsing remediations.xml file: {}", remediationPath, e);
            throw new AviatorTechnicalException("Error processing remediation.xml file.", e);
        }
    }
}
