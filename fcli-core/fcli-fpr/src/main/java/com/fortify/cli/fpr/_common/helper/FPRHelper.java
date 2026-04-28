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
package com.fortify.cli.fpr._common.helper;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.filter.TagDefinition;
import com.fortify.cli.aviator.fpr.filter.TagValue;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FilterTemplateParser;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Helper class for loading and converting FPR vulnerability data
 * into the fcli output framework's {@link ObjectNode} format.
 */
public final class FPRHelper {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FPRHelper() {}

    public record FprLoadResult(List<Vulnerability> vulnerabilities, Map<String, AuditIssue> auditIssueMap) {}

    /**
     * Parses an FPR file and returns the list of vulnerabilities.
     */
    public static List<Vulnerability> loadVulnerabilities(FprHandle fprHandle) {
        return loadVulnerabilitiesWithAudit(fprHandle).vulnerabilities();
    }

    /**
     * Parses an FPR file and returns both the vulnerabilities and the audit issue map.
     */
    public static FprLoadResult loadVulnerabilitiesWithAudit(FprHandle fprHandle) {
        var auditProcessor = new AuditProcessor(fprHandle);
        Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
        var fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
        var streamingProcessor = new StreamingFVDLProcessor(fprHandle);
        var vulnerabilities = fprProcessor.process(streamingProcessor);
        return new FprLoadResult(vulnerabilities, auditIssueMap);
    }

    /**
     * Converts a {@link Vulnerability} to an {@link ObjectNode} for the output framework.
     * Field names are chosen to align with SSC issue field names where possible.
     */
    public static ObjectNode toObjectNode(Vulnerability vuln) {
        var node = MAPPER.createObjectNode();
        node.put("instanceId", vuln.getInstanceID());
        node.put("category", vuln.getCategory());
        node.put("kingdom", vuln.getKingdom());
        node.put("type", vuln.getType());
        node.put("subtype", vuln.getSubType());
        node.put("analyzerName", vuln.getAnalyzerName());
        node.put("severity", vuln.getInstanceSeverity());
        node.put("confidence", vuln.getConfidence());
        node.put("priority", vuln.getPriority());
        node.put("classId", vuln.getClassID());
        node.put("audited", vuln.isAudited());
        node.put("suppressed", vuln.isSuppressed());
        node.put("issueStatus", vuln.getIssueStatus());

        if (vuln.getAccuracy() != null) { node.put("accuracy", vuln.getAccuracy()); }
        if (vuln.getImpact() != null) { node.put("impact", vuln.getImpact()); }
        if (vuln.getProbability() != null) { node.put("probability", vuln.getProbability()); }

        node.put("packageName", vuln.getPackageName());
        node.put("className", vuln.getClassName());
        node.put("functionName", vuln.getFunctionName());
        node.put("sourceFunction", vuln.getSourceFunction());
        node.put("sinkFunction", vuln.getSinkFunction());

        if (vuln.getSource() != null) {
            node.put("primaryFile", vuln.getSource().getFilename());
            node.put("primaryLine", vuln.getSource().getLine());
        } else if (vuln.getSink() != null) {
            node.put("primaryFile", vuln.getSink().getFilename());
            node.put("primaryLine", vuln.getSink().getLine());
        } else if (!vuln.getFiles().isEmpty()) {
            var firstFile = vuln.getFiles().get(0);
            node.put("primaryFile", firstFile.getName());
        }

        node.put("shortDescription", vuln.getShortDescription());
        node.put("lastComment", vuln.getLastComment());

        if (!vuln.getTaintFlags().isEmpty()) {
            ArrayNode flags = MAPPER.createArrayNode();
            vuln.getTaintFlags().forEach(flags::add);
            node.set("taintFlags", flags);
        }

        return node;
    }

    /**
     * Converts a {@link Vulnerability} to a detailed {@link ObjectNode} with all
     * available fields, including explanation, source/sink context, stack traces,
     * knowledge metadata, and DAST fields.
     */
    public static ObjectNode toDetailObjectNode(Vulnerability vuln) {
        var node = toObjectNode(vuln);

        node.put("subcategory", vuln.getSubcategory());
        node.put("explanation", vuln.getExplanation());
        node.put("defaultSeverity", vuln.getDefaultSeverity());
        if (vuln.getLikelihood() != null) { node.put("likelihood", vuln.getLikelihood()); }
        node.put("analysisType", vuln.getAnalysisType());
        node.put("buildId", vuln.getBuildId());

        if (vuln.getSource() != null) {
            var src = MAPPER.createObjectNode();
            src.put("file", vuln.getSource().getFilename());
            src.put("line", vuln.getSource().getLine());
            src.put("code", vuln.getSource().getCode());
            node.set("source", src);
        }
        if (vuln.getSink() != null) {
            var snk = MAPPER.createObjectNode();
            snk.put("file", vuln.getSink().getFilename());
            snk.put("line", vuln.getSink().getLine());
            snk.put("code", vuln.getSink().getCode());
            node.set("sink", snk);
        }

        node.put("sourceContext", vuln.getSourceContext());
        node.put("sinkContext", vuln.getSinkContext());
        node.put("commentUsers", vuln.getCommentUsers());

        if (!vuln.getStackTrace().isEmpty()) {
            ArrayNode traces = MAPPER.createArrayNode();
            for (var trace : vuln.getStackTrace()) {
                ArrayNode traceArray = MAPPER.createArrayNode();
                for (var element : trace) {
                    var elem = MAPPER.createObjectNode();
                    elem.put("file", element.getFilename());
                    elem.put("line", element.getLine());
                    elem.put("code", element.getCode());
                    traceArray.add(elem);
                }
                traces.add(traceArray);
            }
            node.set("traces", traces);
        }

        if (!vuln.getKnowledge().isEmpty()) {
            var knowledgeNode = MAPPER.createObjectNode();
            vuln.getKnowledge().forEach((k, v) -> {
                if (k != null && v != null) { knowledgeNode.put(k, v); }
            });
            node.set("knowledge", knowledgeNode);
        }

        if (vuln.getRequestMethod() != null) { node.put("requestMethod", vuln.getRequestMethod()); }
        if (vuln.getRequestHeaders() != null) { node.put("requestHeaders", vuln.getRequestHeaders()); }
        if (vuln.getRequestParameters() != null) { node.put("requestParameters", vuln.getRequestParameters()); }
        if (vuln.getRequestBody() != null) { node.put("requestBody", vuln.getRequestBody()); }
        if (vuln.getAttackPayload() != null) { node.put("attackPayload", vuln.getAttackPayload()); }
        if (vuln.getAttackType() != null) { node.put("attackType", vuln.getAttackType()); }
        if (vuln.getResponse() != null) { node.put("response", vuln.getResponse()); }
        if (vuln.getVulnerableParameter() != null) { node.put("vulnerableParameter", vuln.getVulnerableParameter()); }

        return node;
    }

    /**
     * Embeds audit history into a detail {@link ObjectNode}: the full comment thread,
     * all current tag values, and the tag change history from ClientAuditTrail.
     */
    public static void embedAuditHistory(ObjectNode node, AuditIssue auditIssue) {
        if (auditIssue == null) { return; }

        node.put("revision", auditIssue.getRevision());
        if (auditIssue.getAssignedUser() != null && !auditIssue.getAssignedUser().isBlank()) {
            node.put("assignedUser", auditIssue.getAssignedUser());
        }

        if (!auditIssue.getTags().isEmpty()) {
            var tagsNode = MAPPER.createObjectNode();
            auditIssue.getTags().forEach((k, v) -> {
                if (k != null) { tagsNode.put(k, v != null ? v : ""); }
            });
            node.set("auditTags", tagsNode);
        }

        if (!auditIssue.getThreadedComments().isEmpty()) {
            ArrayNode commentsArray = MAPPER.createArrayNode();
            for (var comment : auditIssue.getThreadedComments()) {
                var c = MAPPER.createObjectNode();
                c.put("content", comment.getContent());
                c.put("username", comment.getUsername());
                c.put("timestamp", comment.getTimestamp());
                commentsArray.add(c);
            }
            node.set("comments", commentsArray);
        }

        if (!auditIssue.getTagHistory().isEmpty()) {
            ArrayNode historyArray = MAPPER.createArrayNode();
            for (var entry : auditIssue.getTagHistory()) {
                var h = MAPPER.createObjectNode();
                h.put("tagId", entry.getTagId());
                h.put("tagValue", entry.getTagValue());
                h.put("editTime", entry.getEditTime());
                h.put("username", entry.getUsername());
                historyArray.add(h);
            }
            node.set("tagHistory", historyArray);
        }
    }
    /**
     * Loads the FPR's filter template (if present), exposing tag definitions
     * for resolving custom-tag names and their valid values. Returns an empty
     * Optional if the FPR has no filtertemplate.xml.
     */
    public static java.util.Optional<FilterTemplate> loadFilterTemplate(FprHandle fprHandle) {
        var auditProcessor = new com.fortify.cli.aviator.fpr.processor.AuditProcessor(fprHandle);
        auditProcessor.processAuditXML();
        return new FilterTemplateParser(fprHandle, auditProcessor).parseFilterTemplate();
    }

    /**
     * Resolves a user-supplied tag name (or GUID) and value to the canonical
     * tagId / tagValue pair for use with AuditProcessor. Tag and value lookups
     * are case-insensitive. If the tag is not found in the filter template,
     * the input is treated as a raw GUID. If the value is not in the tag's
     * defined values and the tag is not extensible, throws IllegalArgumentException.
     */
    public static java.util.Map.Entry<String, String> resolveCustomTag(
            FilterTemplate filterTemplate, String tagNameOrId, String value) {
        if (tagNameOrId == null || tagNameOrId.isBlank()) {
            throw new IllegalArgumentException("Tag name/id must not be blank");
        }
        if (value == null) {
            throw new IllegalArgumentException("Tag value must not be null for tag '" + tagNameOrId + "'");
        }
        if (filterTemplate == null || filterTemplate.getTagDefinitions() == null) {
            return java.util.Map.entry(tagNameOrId, value);
        }
        TagDefinition match = null;
        for (var def : filterTemplate.getTagDefinitions()) {
            if (tagNameOrId.equalsIgnoreCase(def.getName()) || tagNameOrId.equalsIgnoreCase(def.getId())) {
                match = def;
                break;
            }
        }
        if (match == null) {
            return java.util.Map.entry(tagNameOrId, value);
        }
        if (match.getValues() != null) {
            for (TagValue tv : match.getValues()) {
                if (tv.getValue() != null && tv.getValue().equalsIgnoreCase(value)) {
                    return java.util.Map.entry(match.getId(), tv.getValue());
                }
            }
        }
        if (!match.isExtensible()) {
            var allowed = match.getValues() == null ? java.util.List.<String>of()
                    : match.getValues().stream().map(TagValue::getValue).toList();
            throw new IllegalArgumentException("Invalid value '" + value + "' for tag '"
                    + match.getName() + "'; valid values: " + String.join(", ", allowed));
        }
        return java.util.Map.entry(match.getId(), value);
    }
}