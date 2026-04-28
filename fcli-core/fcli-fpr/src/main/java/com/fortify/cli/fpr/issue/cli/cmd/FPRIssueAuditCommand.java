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
package com.fortify.cli.fpr.issue.cli.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "audit")
public class FPRIssueAuditCommand extends AbstractOutputCommand {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    // Canonical SSC analysis tag values; lookup is case-insensitive.
    private static final Map<String, String> VALID_ANALYSIS_VALUES;
    static {
        VALID_ANALYSIS_VALUES = new LinkedHashMap<>();
        for (var v : new String[] {
                Constants.NOT_AN_ISSUE,
                Constants.EXPLOITABLE,
                Constants.SUSPICIOUS,
                Constants.RELIABILITY_ISSUE,
                Constants.FALSE_POSITIVE,
                Constants.BAD_PRACTICE
        }) {
            VALID_ANALYSIS_VALUES.put(v.toLowerCase(), v);
        }
    }

    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--instance-ids"}, required = true, split = ",", order = 2)
    private List<String> instanceIds;

    @Option(names = {"--analysis"}, required = true, order = 3)
    private String analysis;

    @Option(names = {"--comment"}, order = 4)
    private String comment;

    @Option(names = {"--suppress"}, order = 5)
    private boolean suppress;

    @Option(names = {"--user"}, order = 6)
    private String user;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        var canonicalAnalysis = validateAnalysis(analysis);
        var username = resolveUsername();
        var uniqueIds = dedupePreservingOrder(instanceIds);
        var results = applyAudits(uniqueIds, canonicalAnalysis, username);
        return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .streamSupplier(results::stream)
                .build();
    }

    private List<ObjectNode> applyAudits(List<String> ids, String canonicalAnalysis, String username) {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var auditProcessor = new AuditProcessor(fprHandle);
            auditProcessor.processAuditXML();
            var results = new ArrayList<ObjectNode>(ids.size());
            boolean anyChanged = false;
            for (var id : ids) {
                boolean changed = auditProcessor.auditIssue(id, Constants.ANALYSIS_TAG_ID,
                        canonicalAnalysis, comment, username, suppress);
                anyChanged |= changed;
                results.add(buildResultRow(id, canonicalAnalysis, username, changed));
            }
            if (anyChanged) {
                auditProcessor.saveAuditXml();
            }
            return results;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error processing FPR file", e);
        }
    }

    private ObjectNode buildResultRow(String id, String canonicalAnalysis, String username, boolean changed) {
        var row = MAPPER.createObjectNode();
        row.put("instanceId", id);
        row.put("analysis", canonicalAnalysis);
        row.put("comment", comment != null ? comment : "");
        row.put("suppressed", suppress);
        row.put("user", username);
        row.put("__action__", changed ? "AUDITED" : "UNCHANGED");
        return row;
    }

    private static List<String> dedupePreservingOrder(List<String> ids) {
        var seen = new LinkedHashSet<String>();
        for (var id : ids) {
            if (id != null && !id.isBlank()) {
                seen.add(id.trim());
            }
        }
        if (seen.isEmpty()) {
            throw new FcliSimpleException("--instance-ids must contain at least one non-blank value");
        }
        return new ArrayList<>(seen);
    }

    private String validateAnalysis(String value) {
        if (value == null) { return null; }
        var canonical = VALID_ANALYSIS_VALUES.get(value.toLowerCase());
        if (canonical == null) {
            throw new FcliSimpleException("Invalid --analysis value '" + value
                    + "'; valid values: " + String.join(", ", VALID_ANALYSIS_VALUES.values()));
        }
        return canonical;
    }

    private String resolveUsername() {
        if (user != null && !user.isBlank()) { return user; }
        var sysUser = System.getProperty("user.name");
        return (sysUser != null && !sysUser.isBlank()) ? sysUser : "fcli";
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}