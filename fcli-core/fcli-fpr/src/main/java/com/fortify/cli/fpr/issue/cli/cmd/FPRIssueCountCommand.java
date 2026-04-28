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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.producer.IObjectNodeProducer;
import com.fortify.cli.common.json.producer.ObjectNodeProducerApplyFrom;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FPRHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "count")
public class FPRIssueCountCommand extends AbstractOutputCommand {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Override
    protected IObjectNodeProducer getObjectNodeProducer() {
        List<Vulnerability> vulnerabilities = loadVulnerabilities();
        Map<String, long[]> counts = new LinkedHashMap<>();
        for (var vuln : vulnerabilities) {
            var category = vuln.getCategory() != null ? vuln.getCategory() : "Unknown";
            counts.computeIfAbsent(category, k -> new long[3]);
            long[] c = counts.get(category);
            c[0]++;
            if (vuln.isAudited()) { c[1]++; }
            if (vuln.isSuppressed()) { c[2]++; }
        }
        int total = vulnerabilities.size();
        return streamingObjectNodeProducerBuilder(ObjectNodeProducerApplyFrom.SPEC)
                .streamSupplier(() -> toStream(counts, total))
                .build();
    }

    private List<Vulnerability> loadVulnerabilities() {
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            return FPRHelper.loadVulnerabilities(fprHandle);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error processing FPR file", e);
        }
    }

    private Stream<ObjectNode> toStream(Map<String, long[]> counts, int total) {
        var summary = counts.entrySet().stream().map(e -> {
            var node = MAPPER.createObjectNode();
            node.put("category", e.getKey());
            node.put("total", e.getValue()[0]);
            node.put("audited", e.getValue()[1]);
            node.put("suppressed", e.getValue()[2]);
            return node;
        });
        var totalNode = MAPPER.createObjectNode();
        totalNode.put("category", "TOTAL");
        totalNode.put("total", total);
        totalNode.put("audited", counts.values().stream().mapToLong(c -> c[1]).sum());
        totalNode.put("suppressed", counts.values().stream().mapToLong(c -> c[2]).sum());
        return Stream.concat(summary, Stream.of(totalNode));
    }

    @Override
    public boolean isSingular() {
        return false;
    }
}
