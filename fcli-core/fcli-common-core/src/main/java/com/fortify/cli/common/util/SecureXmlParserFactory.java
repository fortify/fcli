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
package com.fortify.cli.common.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;

public final class SecureXmlParserFactory {
    private SecureXmlParserFactory() {}

    /** Creates a fresh DOM factory that permits DOCTYPE declarations without resolving external resources. */
    public static DocumentBuilderFactory newDocumentBuilderFactory(boolean namespaceAware)
            throws ParserConfigurationException {
        return newDocumentBuilderFactory(namespaceAware, false);
    }

    /** Creates a fresh DOM factory that rejects all DOCTYPE declarations. */
    public static DocumentBuilderFactory newDocumentBuilderFactoryWithoutDoctype(boolean namespaceAware)
            throws ParserConfigurationException {
        return newDocumentBuilderFactory(namespaceAware, true);
    }

    private static DocumentBuilderFactory newDocumentBuilderFactory(boolean namespaceAware, boolean disallowDoctype)
            throws ParserConfigurationException {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", disallowDoctype);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(namespaceAware);
        return factory;
    }

    /** Creates a fresh streaming factory with DTD processing and external entity support disabled. */
    public static XMLInputFactory newXmlInputFactory() {
        var factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory;
    }
}