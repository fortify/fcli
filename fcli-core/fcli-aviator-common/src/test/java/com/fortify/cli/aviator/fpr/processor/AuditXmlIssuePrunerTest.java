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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class AuditXmlIssuePrunerTest {
    private static final String AUDIT_NS = "xmlns://www.fortify.com/schema/audit";

    @Test
    void retainOnlyKeepsListedIdsAndRemovesBlank() throws Exception {
        Document doc = parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="keep-me" revision="0"/>
                    <ns2:Issue instanceId="drop-me" revision="0"/>
                    <ns2:Issue instanceId="" revision="0"/>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS));

        int removed = AuditXmlIssuePruner.retainOnly(doc, Set.of("keep-me"));
        assertEquals(2, removed);
        assertEquals(Set.of("keep-me"), instanceIds(doc));
    }

    @Test
    void retainOnlyWithEmptySetRemovesAllIssues() throws Exception {
        Document doc = parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="a" revision="0"/>
                    <ns2:Issue instanceId="b" revision="0"/>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS));

        int removed = AuditXmlIssuePruner.retainOnly(doc, Set.of());
        assertEquals(2, removed);
        assertTrue(instanceIds(doc).isEmpty());
    }

    @Test
    void retainOnlyNullDocumentIsNoOp() {
        assertEquals(0, AuditXmlIssuePruner.retainOnly(null, Set.of("x")));
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Set<String> instanceIds(Document doc) {
        NodeList nodes = doc.getElementsByTagNameNS(AUDIT_NS, "Issue");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            ids.add(((Element) nodes.item(i)).getAttribute("instanceId"));
        }
        return ids;
    }
}
