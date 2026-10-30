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
package com.fortify.cli.aviator.audit.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.filter.FilterTemplate;
import com.fortify.cli.common.fpr.filter.TagDefinition;
import com.fortify.cli.common.fpr.filter.TagValue;
import com.fortify.cli.common.fpr.processor.FilterTemplateCustomizer;

import lombok.Getter;

/**
 * Adds the Aviator, FoD and Auditor Status tag definitions to a parsed filter template and keeps
 * the resulting DOM so that {@link AuditProcessor} can write it back into the FPR.
 * <p>
 * Instances are single-use and stateful: {@link #getFilterTemplateDocument()} returns {@code null}
 * until the FPR parser has invoked {@link #customize}, which it always does after parsing
 * {@code filtertemplate.xml}.
 */
public final class AviatorFilterTemplateCustomizer implements FilterTemplateCustomizer {
    @Getter private Document filterTemplateDocument;

    @Override
    public void customize(FilterTemplate filterTemplate, Document doc) {
        addMissingTagDefinitions(filterTemplate, doc);
        this.filterTemplateDocument = doc;
    }

    private boolean addMissingTagDefinitions(FilterTemplate filterTemplate, Document doc) {
        String namespaceURI = doc.getDocumentElement().getNamespaceURI();
        Element rootElement = doc.getDocumentElement();

        NodeList tagDefNodes = rootElement.getElementsByTagNameNS(namespaceURI != null ? namespaceURI : "", "TagDefinition");
        Node lastTagDefNode = tagDefNodes.getLength() > 0 ? tagDefNodes.item(tagDefNodes.getLength() - 1) : null;

        boolean needsUpdate = ensureTagDefinitionPresent(filterTemplate, Constants.AVIATOR_PREDICTION_TAG_ID, "Aviator prediction", Arrays.asList(Constants.AVIATOR_NOT_AN_ISSUE, Constants.AVIATOR_REMEDIATION_REQUIRED, Constants.AVIATOR_UNSURE, Constants.AVIATOR_EXCLUDED, Constants.AVIATOR_LIKELY_TP, Constants.AVIATOR_LIKELY_FP), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.AVIATOR_STATUS_TAG_ID, "Aviator status", Arrays.asList(Constants.PROCESSED_BY_AVIATOR), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.FOD_TAG_ID, "FoD", Arrays.asList(Constants.PENDING_REVIEW, Constants.FALSE_POSITIVE, Constants.EXPLOITABLE, Constants.SUSPICIOUS, Constants.SANITIZED), doc, namespaceURI, lastTagDefNode);
        needsUpdate |= ensureTagDefinitionPresent(filterTemplate, Constants.AUDITOR_STATUS_TAG_ID, "Auditor Status", Arrays.asList(Constants.PENDING_REVIEW, Constants.NOT_AN_ISSUE, Constants.UNSURE, Constants.REMEDIATION_REQUIRED, Constants.PROPOSED_NOT_AN_ISSUE, Constants.SUSPICIOUS), doc, namespaceURI, lastTagDefNode);

        return needsUpdate;
    }

    private boolean ensureTagDefinitionPresent(FilterTemplate filterTemplate, String tagId, String tagName, List<String> tagValues, Document doc, String namespaceURI, Node insertAfterNode) {
        Optional<TagDefinition> existingTag = filterTemplate.getTagDefinitions().stream().filter(t -> tagId.equalsIgnoreCase(t.getId())).findFirst();

        if (!existingTag.isPresent()) {
            TagDefinition newTagDef = createTagDefinition(tagId, tagName, tagValues);
            filterTemplate.getTagDefinitions().add(newTagDef);

            Element rootElement = doc.getDocumentElement();
            Element newTagDefElement = doc.createElementNS(namespaceURI, "TagDefinition");
            newTagDefElement.setAttribute("id", newTagDef.getId());
            newTagDefElement.setAttribute("type", "user");
            newTagDefElement.setAttribute("extensible", "false");
            newTagDefElement.setAttribute("hidden", "false");
            newTagDefElement.setAttribute("objectVersion", "0");
            newTagDefElement.setAttribute("valueType", "LIST");

            Element nameElement = doc.createElementNS(namespaceURI, "name");
            nameElement.setTextContent(newTagDef.getName());
            newTagDefElement.appendChild(nameElement);

            Element descriptionElement = doc.createElementNS(namespaceURI, "Description");
            newTagDefElement.appendChild(descriptionElement);

            for (String value : newTagDef.getTagValuesAsString()) {
                Element valueElement = doc.createElementNS(namespaceURI, "value");
                valueElement.setAttribute("id", String.valueOf(newTagDef.getTagValuesAsString().indexOf(value)));
                valueElement.setAttribute("hidden", "false");
                valueElement.setTextContent(value);
                newTagDefElement.appendChild(valueElement);
            }
            if (insertAfterNode != null) {
                rootElement.insertBefore(newTagDefElement, insertAfterNode.getNextSibling());
            } else {
                rootElement.appendChild(newTagDefElement);
            }
            return true;
        }
        return false;
    }

    private TagDefinition createTagDefinition(String id, String name, List<String> values) {
        TagDefinition tagDefinition = new TagDefinition();
        tagDefinition.setId(id);
        tagDefinition.setName(name);
        tagDefinition.setValues(values.stream().map(value -> {
            TagValue tagValue = new TagValue();
            tagValue.setValue(value);
            tagValue.setId(String.valueOf(values.indexOf(value)));
            tagValue.setHidden(false);
            return tagValue;
        }).collect(Collectors.toList()));
        return tagDefinition;
    }
}
