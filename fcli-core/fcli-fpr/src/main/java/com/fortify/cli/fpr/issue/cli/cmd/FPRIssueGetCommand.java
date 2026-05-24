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
import java.util.Set;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fpr._common.cli.mixin.FPRFileMixin;
import com.fortify.cli.fpr._common.helper.FPRHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class FPRIssueGetCommand extends AbstractOutputCommand implements IJsonNodeSupplier {
    private static final Set<String> VALID_EMBEDS = Set.of("history");

    @Getter @Mixin private OutputHelperMixins.Get outputHelper;
    @Mixin private FPRFileMixin fprFileMixin;

    @Option(names = {"--instance-id"}, required = true, order = 2)
    private String instanceId;

    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--embed"}, split = ",", order = 3)
    private Set<String> embed;

    @Override
    public ObjectNode getJsonNode() {
        validateEmbed();
        try (var fprHandle = fprFileMixin.createFprHandle()) {
            var result = FPRHelper.loadVulnerabilitiesWithAudit(fprHandle);
            var vuln = result.vulnerabilities().stream()
                    .filter(v -> instanceId.equals(v.getInstanceID()))
                    .findFirst()
                    .orElseThrow(() -> new FcliSimpleException(
                            "Issue with instanceId '" + instanceId + "' not found in FPR file"));
            var node = FPRHelper.toDetailObjectNode(vuln);
            if (hasEmbed("history")) {
                FPRHelper.embedAuditHistory(node, result.auditIssueMap().get(instanceId));
            }
            return node;
        } catch (IOException e) {
            throw new FcliTechnicalException("Error processing FPR file", e);
        }
    }

    private boolean hasEmbed(String name) {
        return embed != null && embed.contains(name);
    }

    private void validateEmbed() {
        if (embed != null) {
            for (var e : embed) {
                if (!VALID_EMBEDS.contains(e)) {
                    throw new FcliSimpleException("Invalid --embed value '" + e
                            + "'; valid values: " + String.join(", ", VALID_EMBEDS));
                }
            }
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
