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
package com.fortify.cli.fod.issue.cli.cmd;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.mcp.MCPInclude;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.util.FoDEnums.AuditorStatusType;
import com.fortify.cli.fod._common.util.FoDEnums.DeveloperStatusType;
import com.fortify.cli.fod._common.util.FoDEnums.VulnerabilitySeverityType;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
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
    // Fields to hold last-run counts so getActionCommandResult() can report status
    private int lastTotalCount = 0;
    private int lastSkippedCount = 0;
    private long lastErrorCount = 0;
    private int lastUpdateCount = 0;
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Mixin private FoDAttributeUpdateOptions.OptionalAttrOption issueAttrsUpdate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Option(names = {"--user"}, required = true)
    protected String user;
    @Option(names = {"--dev-status"}, required = false)
    protected String developerStatus;
    @Option(names = {"--auditor-status"}, required = false)
    protected String auditorStatus;
    @Option(names = {"--severity"}, required = false)
    protected VulnerabilitySeverityType severity;
    @Option(names = {"--comment"}, required = false)
    protected String comment;
    @Option(names = {"--vuln-ids"}, required = true, split=",")
    protected ArrayList<String> vulnIds;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);

        // If vulnIds are provided, filter them against the release vulnerabilities using a helper.
        int issueUpdateCount = 0;
        int totalCount = 0;
        int skippedCount = 0;
        if ( vulnIds != null && !vulnIds.isEmpty() ) {
            var vulnFilterResult = FoDIssueHelper.filterRequestedVulnIds(unirest, releaseDescriptor.getReleaseId(), vulnIds);
            totalCount = vulnFilterResult.totalCount();
            issueUpdateCount = vulnFilterResult.kept().size();
            skippedCount = vulnFilterResult.skipped().size();
            vulnIds = new ArrayList<>(vulnFilterResult.kept());
            if (!vulnFilterResult.skipped().isEmpty()) {
                LOG.debug("Skipped vulnerabilities: {}", vulnFilterResult.skipped());
                vulnFilterResult.skipped().forEach(vid -> LOG.warn("Vulnerability {} not found in release {}, skipping", vid, releaseDescriptor.getReleaseId()));
            }
        }

        Map<String, String> attributeUpdates = issueAttrsUpdate.getAttributes();
        JsonNode jsonAttrs = FoDIssueHelper.buildIssueAttributesNode(unirest, attributeUpdates);

        // Validate auditor and developer status values against attribute picklists
        ResolvedStatuses resolvedStatuses = resolveStatuses(unirest);

        FoDBulkIssueUpdateRequest issueUpdateRequest = buildIssueUpdateRequest(unirest, resolvedStatuses.developerStatusValue(), resolvedStatuses.auditorStatusValue(), jsonAttrs);
        FoDBulkIssueUpdateResponse resp = performUpdate(unirest, releaseDescriptor.getReleaseId(), issueUpdateRequest, totalCount, skippedCount, issueUpdateCount);
        return resp.asObjectNode()
            .put("totalCount", totalCount)
            .put("skippedCount", skippedCount)
            .put("errorCount", lastErrorCount)
            .put("updateCount", issueUpdateCount);
    }

    private record ResolvedStatuses(String developerStatusValue, String auditorStatusValue) {}

    private ResolvedStatuses resolveStatuses(UnirestInstance unirest) {
        String auditorStatusValue = null;
        if ( auditorStatus != null && !auditorStatus.isBlank() ) {
            auditorStatusValue = FoDIssueHelper.resolveStatusValue(unirest, auditorStatus, new String[]{
                "Auditor Status (Non suppressed)", "Auditor Status (Suppressed)"
            }, "auditor-status", AuditorStatusType.values());
        }
        String developerStatusValue = null;
        if ( developerStatus != null && !developerStatus.isBlank() ) {
            developerStatusValue = FoDIssueHelper.resolveStatusValue(unirest, developerStatus, new String[]{
                "Developer Status (Open)", "Developer Status (Closed)"
            }, "developer-status", DeveloperStatusType.values());
        }
        return new ResolvedStatuses(developerStatusValue, auditorStatusValue);
    }

    private FoDBulkIssueUpdateRequest buildIssueUpdateRequest(UnirestInstance unirest, String developerStatusValue, String auditorStatusValue, JsonNode jsonAttrs) {
        return FoDBulkIssueUpdateRequest.builder()
            .user(unirest, user)
            .developerStatus(developerStatusValue)
            .auditorStatus(auditorStatusValue)
            .severity(severity != null ? severity.toString() : null)
            .comment(comment)
            .vulnerabilityIds(vulnIds)
            .attributes(jsonAttrs)
            .build().validate();
    }

    private FoDBulkIssueUpdateResponse performUpdate(UnirestInstance unirest, String releaseId, FoDBulkIssueUpdateRequest request, int totalCount, int skippedCount, int issueUpdateCount) {
        LOG.debug("Updating issues: {}", vulnIds);
        FoDBulkIssueUpdateResponse resp = FoDIssueHelper.updateIssues(unirest, releaseId, request);
        long errorCount = resp.getResults().stream().filter(r -> r.getErrorCode() != 0).count();
        resp.setIssueCount(resp.getResults().size());
        resp.setErrorCount(errorCount);
        // Store last-run counts for action result reporting
        lastTotalCount = totalCount;
        lastSkippedCount = skippedCount;
        lastErrorCount = errorCount;
        lastUpdateCount = issueUpdateCount;
        LOG.debug("Response: {}", resp.getResults());
        return resp;
    }

    @Override
    public String getActionCommandResult() {
        if ( lastErrorCount>0 ) { return "UPDATED_WITH_ERRORS"; }
        if ( lastSkippedCount>0 ) { return "UPDATED_WITH_SKIPPED"; }
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }

}
