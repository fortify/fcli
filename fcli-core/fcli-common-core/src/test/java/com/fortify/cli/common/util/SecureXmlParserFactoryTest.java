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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class SecureXmlParserFactoryTest {
    @Test
    void domUsesSecurePolicyAndFreshFactories() throws Exception {
        var factory = SecureXmlParserFactory.newDocumentBuilderFactory(true);
        assertNotSame(factory, SecureXmlParserFactory.newDocumentBuilderFactory(true));
        assertFalse(factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl"));
        assertFalse(factory.getFeature("http://xml.org/sax/features/external-general-entities"));
        assertFalse(factory.getFeature("http://xml.org/sax/features/external-parameter-entities"));
        assertFalse(factory.getFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd"));
        assertTrue(factory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING));
        assertFalse(factory.isXIncludeAware());
        assertFalse(factory.isExpandEntityReferences());
        assertTrue(factory.isNamespaceAware());
    }

    @Test
    void domAllowsHarmlessDoctypeAndPreservesNamespaceChoice() throws Exception {
        var xml = "<!DOCTYPE root><root xmlns='urn:test'>safe</root>";
        var aware = parse(SecureXmlParserFactory.newDocumentBuilderFactory(true), xml);
        assertEquals("urn:test", aware.getDocumentElement().getNamespaceURI());
        assertEquals("safe", aware.getDocumentElement().getTextContent());
        var unaware = parse(SecureXmlParserFactory.newDocumentBuilderFactory(false), xml);
        assertNull(unaware.getDocumentElement().getNamespaceURI());
    }

    @Test
    void domDoesNotResolveExternalEntitiesOrDtd() throws Exception {
        for (var declaration : new String[] {
                "<!DOCTYPE root SYSTEM 'file:///nonexistent-external.dtd'>",
                "<!DOCTYPE root [<!ENTITY external SYSTEM 'file:///nonexistent-secret'>]>",
                "<!DOCTYPE root [<!ENTITY % external SYSTEM 'file:///nonexistent-external.dtd'>%external;]>" }) {
            var body = declaration.contains("<!ENTITY external") ? "&external;" : "safe";
            var document = parse(SecureXmlParserFactory.newDocumentBuilderFactory(true),
                    declaration + "<root>" + body + "</root>");
            assertEquals(body.equals("safe") ? "safe" : "", document.getDocumentElement().getTextContent());
        }
    }

    @Test
    void strictDomRejectsDoctype() throws Exception {
        var factory = SecureXmlParserFactory.newDocumentBuilderFactoryWithoutDoctype(false);
        assertThrows(SAXException.class, () -> parse(factory, "<!DOCTYPE root><root/>"));
    }

    @Test
    void staxDoesNotResolveExternalDtdAndPreservesNamespaces() throws Exception {
        var factory = SecureXmlParserFactory.newXmlInputFactory();
        assertNotSame(factory, SecureXmlParserFactory.newXmlInputFactory());
        assertEquals(false, factory.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertEquals(false, factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new AssertionError("External XML resource requested");
        });
        var reader = factory.createXMLStreamReader(new StringReader(
                "<!DOCTYPE root SYSTEM 'file:///nonexistent-external.dtd'><root xmlns='urn:test'>safe</root>"));
        try {
            while (reader.hasNext() && !reader.isStartElement()) {
                reader.next();
            }
            assertEquals("urn:test", reader.getNamespaceURI());
            assertEquals("safe", reader.getElementText());
        } finally {
            reader.close();
        }
    }

    @Test
    void staxDoesNotResolveExternalEntity() throws Exception {
        var factory = SecureXmlParserFactory.newXmlInputFactory();
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            throw new AssertionError("External XML resource requested");
        });
        var reader = factory.createXMLStreamReader(new StringReader(
                "<!DOCTYPE root [<!ENTITY external SYSTEM 'file:///nonexistent-secret'>]><root>&external;</root>"));
        try {
            assertThrows(XMLStreamException.class, () -> {
                while (reader.hasNext()) {
                    reader.next();
                }
            });
        } finally {
            reader.close();
        }
    }

    private Document parse(DocumentBuilderFactory factory, String xml) throws Exception {
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            throw new AssertionError("External XML resource requested");
        });
        return builder.parse(new InputSource(new StringReader(xml)));
    }
}