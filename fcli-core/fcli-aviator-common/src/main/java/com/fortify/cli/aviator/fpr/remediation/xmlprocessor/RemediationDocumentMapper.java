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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.fpr.remediation.model.FileChange;
import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.fpr.remediation.model.Remediation;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationDocument;

/**
 * Phase 2: maps a DOM {@link Document} into the domain model, without any additional logic —
 * no normalization, no comparison codes, no classification, and (deliberately) no validation:
 * fields whose element/attribute is absent are stored as {@code null}/blank rather than
 * throwing, so that phase 3 code can validate lazily at the exact point of use, exactly as the
 * original single-class implementation did.
 */
public final class RemediationDocumentMapper {
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";

    public RemediationDocument map(Document remediationDoc) {
        NodeList remediationNodes = remediationDoc.getElementsByTagNameNS(NAMESPACE_URI, "Remediation");
        List<Remediation> remediations = new ArrayList<>();
        for (int i = 0; i < remediationNodes.getLength(); i++) {
            remediations.add(mapRemediation((Element) remediationNodes.item(i)));
        }
        return new RemediationDocument(remediations);
    }

    private Remediation mapRemediation(Element remediationElement) {
        String instanceId = remediationElement.getAttribute("instanceId");
        NodeList fileChangesNodes = remediationElement.getElementsByTagNameNS(NAMESPACE_URI, "FileChanges");
        List<FileChange> fileChanges = new ArrayList<>();
        for (int i = 0; i < fileChangesNodes.getLength(); i++) {
            fileChanges.add(mapFileChange((Element) fileChangesNodes.item(i)));
        }
        return new Remediation(instanceId, fileChanges);
    }

    private FileChange mapFileChange(Element fileChangesElement) {
        String filename = optionalElementText(fileChangesElement, "Filename");
        String hash = optionalElementText(fileChangesElement, "Hash");
        NodeList changeNodes = fileChangesElement.getElementsByTagNameNS(NAMESPACE_URI, "Change");
        List<Hunk> hunks = new ArrayList<>();
        for (int i = 0; i < changeNodes.getLength(); i++) {
            hunks.add(mapHunk((Element) changeNodes.item(i)));
        }
        return new FileChange(filename, hash, hunks);
    }

    private Hunk mapHunk(Element changeElement) {
        String lineFrom = optionalElementText(changeElement, "LineFrom");
        String lineTo = optionalElementText(changeElement, "LineTo");
        Element contextElement = optionalElement(changeElement, "Context");
        String contextText = contextElement == null ? null : contextElement.getTextContent();
        String contextBefore = contextElement == null ? null : contextElement.getAttribute("before");
        String contextAfter = contextElement == null ? null : contextElement.getAttribute("after");
        String originalCode = optionalElementText(changeElement, "OriginalCode");
        String newCode = optionalElementText(changeElement, "NewCode");
        return new Hunk(lineFrom, lineTo, contextText, contextBefore, contextAfter, originalCode, newCode);
    }

    private String optionalElementText(Element parent, String elementName) {
        Element element = optionalElement(parent, elementName);
        return element == null ? null : element.getTextContent();
    }

    private Element optionalElement(Element parent, String elementName) {
        NodeList nodes = parent.getElementsByTagNameNS(NAMESPACE_URI, elementName);
        return nodes.getLength() == 0 ? null : (Element) nodes.item(0);
    }
}
