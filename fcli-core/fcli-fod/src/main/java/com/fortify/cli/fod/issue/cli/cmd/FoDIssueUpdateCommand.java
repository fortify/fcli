/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.fod.issue.cli.cmd;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.mcp.MCPInclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.util.FoDEnums.AuditorStatusType;
import com.fortify.cli.fod._common.util.FoDEnums.DeveloperStatusType;
import com.fortify.cli.fod._common.util.FoDEnums.VulnerabilitySeverityType;
import com.fortify.cli.fod.issue.helper.FoDBulkIssueUpdateRequest;
import com.fortify.cli.fod.issue.helper.FoDBulkIssueUpdateResponse;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@MCPInclude
@Command(name = OutputHelperMixins.Update.CMD_NAME)
public class FoDIssueUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(FoDIssueUpdateCommand.class);
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"--user"}, required = true)
    protected String user;
    @Option(names = {"--dev-status"}, required = false)
    protected DeveloperStatusType developerStatus;
    @Option(names = {"--auditor-status"}, required = false)
    protected AuditorStatusType auditorStatus;
    @Option(names = {"--severity"}, required = false)
    protected VulnerabilitySeverityType severity;
    @Option(names = {"--comment"}, required = false)
    protected String comment;
    @Option(names = {"--vuln-ids"}, required = true, split=",")
    protected ArrayList<String> vulnIds;

    private long errorCount = 0;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);

        FoDBulkIssueUpdateRequest issueUpdateRequest = FoDBulkIssueUpdateRequest.builder()
            .user(unirest, user)
            .developerStatus(developerStatus != null ? developerStatus.getValue() : null)
            .auditorStatus(auditorStatus != null ? auditorStatus.getValue() : null)
            .severity(severity != null ? severity.toString() : null)
            .comment(comment)
            .vulnerabilityIds(vulnIds)
            .build().validate();

        LOG.debug("Updating issues: {}", vulnIds.toString());
        FoDBulkIssueUpdateResponse resp = FoDIssueHelper.updateIssues(unirest, releaseDescriptor.getReleaseId(), issueUpdateRequest);
        errorCount = resp.getResults()
            .stream()
            .filter(r -> r.getErrorCode() != 0)
            .count();
        resp.setIssueCount(resp.getResults().size());
        resp.setErrorCount(errorCount);
        LOG.debug("Response: {}", resp.getResults().toString());
        return resp.asObjectNode().put("issueCount", resp.getResults().size()).put("errorCount", errorCount);
    }

    @Override
    public String getActionCommandResult() {
        return "ISSUES_UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

}