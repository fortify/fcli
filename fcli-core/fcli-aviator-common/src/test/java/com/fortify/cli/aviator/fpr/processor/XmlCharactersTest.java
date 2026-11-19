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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class XmlCharactersTest {
    @Test
    void controlCharactersAreInvalidButWhitespaceIsNot() {
        assertFalse(XmlCharacters.isValid(0x18));
        assertFalse(XmlCharacters.isValid(0x0));
        assertFalse(XmlCharacters.isValid(0xB));
        assertFalse(XmlCharacters.isValid(0xFFFE));
        assertFalse(XmlCharacters.isValid(0xD800));
        assertTrue(XmlCharacters.isValid('\t'));
        assertTrue(XmlCharacters.isValid('\n'));
        assertTrue(XmlCharacters.isValid('\r'));
        assertTrue(XmlCharacters.isValid(' '));
        assertTrue(XmlCharacters.isValid(0x1F600));
    }

    @Test
    void stripRemovesOnlyInvalidCharactersAndKeepsEmoji() {
        assertEquals("ab\tc\n\u00e9\uD83D\uDE00", XmlCharacters.strip("a\u0018b\tc\n\u0000\u00e9\uD83D\uDE00"));
    }

    @Test
    void stripRemovesALoneSurrogate() {
        assertEquals("ab", XmlCharacters.strip("a\uD800b"));
    }

    @Test
    void stripReturnsTheSameStringWhenThereIsNothingToRemove() {
        String text = "clean text";
        assertSame(text, XmlCharacters.strip(text));
        assertFalse(XmlCharacters.hasInvalid(text));
        assertFalse(XmlCharacters.hasInvalid(null));
    }

    @Test
    void describeListsEachInvalidCodePointOnce() {
        assertEquals("U+0018, U+0001", XmlCharacters.describeInvalid("a\u0018b\u0001c\u0018"));
        assertEquals("", XmlCharacters.describeInvalid("clean"));
    }

    @Test
    void stripInvalidCleansTextCdataAndAttributesSoTheDocumentSerializesAndParses() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("Root");
        doc.appendChild(root);
        root.setAttribute("name", "n\u0018ame");
        Element text = doc.createElement("Text");
        text.setTextContent("a\u0018b");
        root.appendChild(text);
        Element cdata = doc.createElement("Cdata");
        cdata.appendChild(doc.createCDATASection("int x;\u0018 // y\u0001"));
        root.appendChild(cdata);

        assertEquals(4, XmlCharacters.stripInvalid(doc));

        assertEquals("name", root.getAttribute("name"));
        assertEquals("ab", text.getTextContent());
        assertEquals("int x; // y", cdata.getTextContent());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter xml = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(xml));
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.toString().getBytes("UTF-8")));
    }

    @Test
    void stripInvalidLeavesACleanDocumentAlone() throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = doc.createElement("Root");
        root.setTextContent("clean");
        doc.appendChild(root);

        assertEquals(0, XmlCharacters.stripInvalid(doc));
        assertEquals("clean", root.getTextContent());
    }
}
