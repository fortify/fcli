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
import com.fortify.cli.common.json.JsonHelper;
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
import com.fortify.cli.fod.issue.helper.FoDIssueAttributeHelper;
import com.fortify.cli.fod.issue.helper.FoDIssueHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
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

    @Option(names = {"--user"}, required = false)
    protected String user;
    @Option(names = {"--dev-status"}, required = false)
    protected String developerStatus;
    @Option(names = {"--auditor-status"}, required = false)
    protected String auditorStatus;
    @Option(names = {"--severity"}, required = false)
    protected VulnerabilitySeverityType severity;
    @Option(names = {"--comment"}, required = false)
    protected String comment;
    @ArgGroup(exclusive = true, multiplicity = "1")
    private VulnSelectionArgs vulnSelection;

    static class VulnSelectionArgs {
        @Option(names = {"--vuln-ids", "--issue-ids"}, required = true, split=",")
        ArrayList<String> vulnIds;
        @Option(names = {"--include-all", "--all"}, required = true)
        boolean includeAllVulnerabilities;
    }

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);

        Map<String, String> attributeUpdates = issueAttrsUpdate.getAttributes();
        var issueAttrHelper = new FoDIssueAttributeHelper(unirest);
        JsonNode jsonAttrs = issueAttrHelper.buildAttributesNode(attributeUpdates);
        ResolvedStatuses resolvedStatuses = resolveStatuses(issueAttrHelper);

        if (vulnSelection.includeAllVulnerabilities) {
            FoDBulkIssueUpdateRequest issueUpdateRequest = buildIssueUpdateRequest(unirest, resolvedStatuses.developerStatusValue(), resolvedStatuses.auditorStatusValue(), jsonAttrs, null, true);
            FoDBulkIssueUpdateResponse resp = performUpdate(unirest, releaseDescriptor.getReleaseId(), issueUpdateRequest, 0, 0, 0, null);
            int updateCount = (int) resp.getIssueCount();
            lastTotalCount = updateCount;
            lastUpdateCount = updateCount;
            return resp.asObjectNode()
                .put("totalCount", updateCount)
                .put("skippedCount", 0)
                .put("errorCount", lastErrorCount)
                .put("updateCount", updateCount);
        } else {
            var vulnFilterResult = FoDIssueHelper.filterRequestedVulnIds(unirest, releaseDescriptor.getReleaseId(), vulnSelection.vulnIds);
            int totalCount = vulnFilterResult.totalCount();
            int issueUpdateCount = vulnFilterResult.kept().size();
            int skippedCount = vulnFilterResult.skipped().size();
            ArrayList<String> effectiveVulnIds = new ArrayList<>(vulnFilterResult.kept());
            if (!vulnFilterResult.skipped().isEmpty()) {
                LOG.debug("Skipped vulnerabilities: {}", vulnFilterResult.skipped());
                vulnFilterResult.skipped().forEach(vid -> LOG.warn("Vulnerability {} not found in release {}, skipping", vid, releaseDescriptor.getReleaseId()));
            }
            if (effectiveVulnIds.isEmpty()) {
                return createNoOpResponse(totalCount, skippedCount, issueUpdateCount);
            }
            FoDBulkIssueUpdateRequest issueUpdateRequest = buildIssueUpdateRequest(unirest, resolvedStatuses.developerStatusValue(), resolvedStatuses.auditorStatusValue(), jsonAttrs, effectiveVulnIds, false);
            FoDBulkIssueUpdateResponse resp = performUpdate(unirest, releaseDescriptor.getReleaseId(), issueUpdateRequest, totalCount, skippedCount, issueUpdateCount, effectiveVulnIds);
            return resp.asObjectNode()
                .put("totalCount", totalCount)
                .put("skippedCount", skippedCount)
                .put("errorCount", lastErrorCount)
                .put("updateCount", issueUpdateCount);
        }
    }

    private JsonNode createNoOpResponse(int totalCount, int skippedCount, int issueUpdateCount) {
        lastTotalCount = totalCount;
        lastSkippedCount = skippedCount;
        lastErrorCount = 0;
        lastUpdateCount = issueUpdateCount;
        return JsonHelper.getObjectMapper().createObjectNode()
            .put("totalCount", totalCount)
            .put("skippedCount", skippedCount)
            .put("errorCount", 0)
            .put("updateCount", issueUpdateCount);
    }

    private record ResolvedStatuses(String developerStatusValue, String auditorStatusValue) {}

    private ResolvedStatuses resolveStatuses(FoDIssueAttributeHelper issueAttrHelper) {
        String auditorStatusValue = null;
        if ( auditorStatus != null && !auditorStatus.isBlank() ) {
            auditorStatusValue = issueAttrHelper.resolveStatusValue(auditorStatus, new String[]{
                "Auditor Status (Non suppressed)", "Auditor Status (Suppressed)"
            }, "auditor-status", AuditorStatusType.values());
        }
        String developerStatusValue = null;
        if ( developerStatus != null && !developerStatus.isBlank() ) {
            developerStatusValue = issueAttrHelper.resolveStatusValue(developerStatus, new String[]{
                "Developer Status (Open)", "Developer Status (Closed)"
            }, "developer-status", DeveloperStatusType.values());
        }
        return new ResolvedStatuses(developerStatusValue, auditorStatusValue);
    }

    private FoDBulkIssueUpdateRequest buildIssueUpdateRequest(UnirestInstance unirest, String developerStatusValue, String auditorStatusValue, JsonNode jsonAttrs, ArrayList<String> effectiveVulnIds, boolean includeAllVulnerabilities) {
        return FoDBulkIssueUpdateRequest.builder()
            .user(unirest, user)
            .developerStatus(developerStatusValue)
            .auditorStatus(auditorStatusValue)
            .severity(severity != null ? severity.toString() : null)
            .comment(comment)
            .vulnerabilityIds(effectiveVulnIds)
            .includeAllVulnerabilities(includeAllVulnerabilities ? true : null)
            .attributes(jsonAttrs)
            .build().validate();
    }

    private FoDBulkIssueUpdateResponse performUpdate(UnirestInstance unirest, String releaseId, FoDBulkIssueUpdateRequest request, int totalCount, int skippedCount, int issueUpdateCount, ArrayList<String> effectiveVulnIds) {
        if (effectiveVulnIds != null) { LOG.debug("Updating issues: {}", effectiveVulnIds); }
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
