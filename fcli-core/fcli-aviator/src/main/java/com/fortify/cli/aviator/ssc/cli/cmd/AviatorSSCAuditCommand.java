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
package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.AuditFPR;
import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionRefreshOptions;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetOptionMixin;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "audit")
@DefaultVariablePropertyName("artifactId")
public class AviatorSSCAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private AviatorUserSessionDescriptorSupplier sessionDescriptorSupplier;
    @Mixin private SSCAppVersionRefreshOptions refreshOptions;
    @Mixin private SSCIssueFilterSetOptionMixin filterSetOptions;
    @Option(names = {"--app"}) private String appName;
    @Option(names = {"--tag-mapping"}) private String tagMapping;
    @Option(names = {"--no-filterset"}) private boolean noFilterSet;
    @Option(names = {"--folder"}, split = ",") @DisableTest(DisableTest.TestType.MULTI_OPT_PLURAL_NAME) private List<String> folderNames;
    @Option(names = {"--skip-if-exceeding-quota"}) private boolean skipIfExceedingQuota;
    @Option(names = {"--test-exceeding-quota"}) private boolean testExceedingQuota;
    @Option(names = {"--default-quota-fallback"}) private boolean defaultQuotaFallback;
    @Option(names = {"--folder-priority-order"}, split = ",",
            description = "Custom priority order by folder (comma-separated, highest first). Example: Critical,High,Medium,Low")
    @DisableTest(DisableTest.TestType.MULTI_OPT_PLURAL_NAME)
    private List<String> folderPriorityOrder;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAuditCommand.class);
    private Long checkedQuotaBefore;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        Path downloadedFprPath = null;
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);

            refreshMetricsIfNeeded(unirest, av, logger);

            long auditableIssueCount = AviatorSSCAuditHelper.getAuditableIssueCount(unirest, av, logger, noFilterSet, filterSetOptions, folderNames);
            if (auditableIssueCount == 0) {
                logger.progress("Audit skipped - no auditable issues found matching the specified filters.");
                ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "SKIPPED");
                AviatorSSCAuditHelper.setOperationMessage(result, "No auditable issues found matching the specified filters");
                return result;
            }

            JsonNode quotaResult = checkQuota(unirest, av, sessionDescriptor, auditableIssueCount, logger);
            if (quotaResult != null) {
                return quotaResult;
            }

            downloadedFprPath = downloadFpr(unirest, av, logger);
            if (downloadedFprPath == null) {
                ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "SKIPPED");
                AviatorSSCAuditHelper.setOperationMessage(result, "No FPR available to audit");
                return result;
            }

            ObjectNode result = (ObjectNode) processFpr(unirest, av, sessionDescriptor.getAviatorToken(), sessionDescriptor.getAviatorUrl(), logger, downloadedFprPath);
            if (checkedQuotaBefore != null) {
                AviatorSSCAuditHelper.setAvailableQuotaBefore(result, checkedQuotaBefore);
            }
            return result;
        } finally {
            if (downloadedFprPath != null) {
                Files.deleteIfExists(downloadedFprPath);
            }
        }
    }

    private void refreshMetricsIfNeeded(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger) {
        if (refreshOptions.isRefresh() && av.isRefreshRequired()) {
            logger.progress("Status: Metrics for application version %s:%s are out of date, starting refresh...", av.getApplicationName(), av.getVersionName());
            SSCJobDescriptor refreshJobDesc = SSCAppVersionHelper.refreshMetrics(unirest, av);
            if (refreshJobDesc != null) {
                SSCJobHelper.waitForJob(unirest, refreshJobDesc, refreshOptions.getRefreshTimeout());
                logger.progress("Status: Metrics refreshed successfully.");
            }
        }
    }

    /**
     * Checks quota constraints when --skip-if-exceeding-quota or --test-exceeding-quota is active.
     * @return a result JsonNode if the audit should be skipped/reported, or null if the audit should proceed.
     */
    private JsonNode checkQuota(UnirestInstance unirest, SSCAppVersionDescriptor av,
            AviatorUserSessionDescriptor sessionDescriptor,
            long auditableIssueCount, AviatorLoggerImpl logger) {
        if (!skipIfExceedingQuota && !testExceedingQuota) {
            return null;
        }

        String effectiveAppName = appName != null ? appName : av.getApplicationName();
        long availableQuota = AviatorSSCAuditHelper.getAvailableQuota(
            sessionDescriptor.getAviatorUrl(), sessionDescriptor.getAviatorToken(),
            effectiveAppName, logger);

        // App not found — behavior depends on --default-quota-fallback
        if (availableQuota == AviatorSSCAuditHelper.QUOTA_APP_NOT_FOUND) {
            availableQuota = handleAppNotFound(sessionDescriptor, effectiveAppName, logger);
            if (availableQuota == AviatorSSCAuditHelper.QUOTA_APP_NOT_FOUND) {
                ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "SKIPPED");
                AviatorSSCAuditHelper.setOperationMessage(result, "Application '" + effectiveAppName + "' not found in Aviator");
                return result;
            }
        }

        // If auditable issue count is unknown (-1), skip quota comparison and proceed with audit
        if (auditableIssueCount < 0) {
            LOG.info("Auditable issue count unknown; skipping quota evaluation for {}:{}.",
                av.getApplicationName(), av.getVersionName());
            return null;
        }

        return evaluateQuota(unirest, av, effectiveAppName, auditableIssueCount, availableQuota, logger);
    }

    /**
     * Handles the case where the application is not found in Aviator.
     * @return the resolved quota (possibly from default), or QUOTA_APP_NOT_FOUND if audit should be skipped.
     */
    private long handleAppNotFound(AviatorUserSessionDescriptor sessionDescriptor,
            String effectiveAppName, AviatorLoggerImpl logger) {
        if (defaultQuotaFallback) {
            logger.progress("Application '%s' not found, using default quota for new applications.", effectiveAppName);
            long defaultQuota = AviatorSSCAuditHelper.getDefaultQuota(
                sessionDescriptor.getAviatorUrl(), sessionDescriptor.getAviatorToken(), logger);
            if (defaultQuota == AviatorSSCAuditHelper.QUOTA_UNKNOWN) {
                if (testExceedingQuota) {
                    // Caller will need to handle this — we return QUOTA_UNKNOWN to signal
                    return AviatorSSCAuditHelper.QUOTA_UNKNOWN;
                }
                logger.progress("Warning: Could not retrieve default quota, proceeding with audit.");
                return AviatorSSCAuditHelper.QUOTA_UNKNOWN;
            }
            return defaultQuota;
        } else {
            logger.progress("Application '%s' does not exist in Aviator.", effectiveAppName);
            return AviatorSSCAuditHelper.QUOTA_APP_NOT_FOUND;
        }
    }

    /**
     * Evaluates the resolved quota against the auditable issue count and returns
     * a result node if audit should be skipped, or null to proceed with the audit.
     */
    private JsonNode evaluateQuota(UnirestInstance unirest, SSCAppVersionDescriptor av,
            String effectiveAppName, long auditableIssueCount, long availableQuota,
            AviatorLoggerImpl logger) {
        if (availableQuota == AviatorSSCAuditHelper.QUOTA_UNKNOWN) {
            if (testExceedingQuota) {
                ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "QUOTA_UNKNOWN");
                AviatorSSCAuditHelper.setOperationMessage(result, "Could not retrieve quota for application '" + effectiveAppName + "'");
                return result;
            }
            logger.progress("Warning: Could not retrieve quota for '%s', proceeding with audit.", effectiveAppName);
        } else if (availableQuota >= 0 && auditableIssueCount > availableQuota) {
            checkedQuotaBefore = availableQuota;
            var topCategories = AviatorSSCAuditHelper.getTopUnauditedCategories(unirest, av, logger, 10);
            String detailedMessage = AviatorSSCAuditHelper.formatQuotaExceededMessage(
                av, auditableIssueCount, availableQuota, topCategories);
            LOG.info(detailedMessage);
            logger.progress("Quota exceeded for %s:%s -- Open issues: %d, Available quota: %d. Audit skipped.",
                av.getApplicationName(), av.getVersionName(), auditableIssueCount, availableQuota);
            return AviatorSSCAuditHelper.buildQuotaExceededResultNode(
                av, auditableIssueCount, availableQuota, topCategories);
        } else if (testExceedingQuota) {
            logger.progress("Quota check passed for %s:%s -- Open issues: %d, Available quota: %s",
                av.getApplicationName(), av.getVersionName(), auditableIssueCount,
                availableQuota < 0 ? "unlimited" : String.valueOf(availableQuota));
            ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "QUOTA_OK");
            AviatorSSCAuditHelper.setOperationMessage(result, String.format("Quota check passed: %d issues, %s quota available",
                auditableIssueCount, availableQuota < 0 ? "unlimited" : String.valueOf(availableQuota)));
            AviatorSSCAuditHelper.setAvailableQuotaBefore(result, availableQuota);
            return result;
        }
        // Quota was checked and audit is proceeding — capture the value for the final output
        checkedQuotaBefore = availableQuota >= 0 ? availableQuota : null;
        return null;
    }

    @SneakyThrows
    private JsonNode processFpr(UnirestInstance unirest, SSCAppVersionDescriptor av, String token, String url, AviatorLoggerImpl logger, Path downloadedFprPath) {
        FPRAuditResult auditResult;

        try (FprHandle fprHandle = new FprHandle(downloadedFprPath)) {
            auditResult = AuditFPR.auditFPR(AuditFprOptions.builder()
                    .fprHandle(fprHandle).token(token).url(url)
                    .appVersion(appName)
                    .sscAppName(av.getApplicationName())
                    .sscAppVersion(av.getVersionName())
                    .logger(logger)
                    .tagMappingPath(tagMapping)
                    .filterSetNameOrId(filterSetOptions.getFilterSetTitleOrId())
                    .noFilterSet(noFilterSet)
                    .folderNames(folderNames)
                    .folderPriorityOrder(folderPriorityOrder)
                    .build());
        } catch (Exception e) {
            LOG.error("FPR audit failed for {}:{}: {}", av.getApplicationName(), av.getVersionName(), e.getMessage(), e);
            ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, null, "FAILED");
            AviatorSSCAuditHelper.setOperationMessage(result, "Audit failed: " + e.getMessage());
            return result;
        }

        String action = auditResult.getStatus();
        logger.progress(AviatorSSCAuditHelper.getProgressMessage(auditResult));

        String artifactId = null;
        if (auditResult.getUpdatedFile() != null && !"SKIPPED".equals(action) && !"FAILED".equals(action)) {
            try {
                artifactId = uploadAuditedFprToSSC(unirest, auditResult.getUpdatedFile(), av);
            } catch (Exception e) {
                LOG.error("Failed to upload audited FPR for {}:{}: {}", av.getApplicationName(), av.getVersionName(), e.getMessage(), e);
                logger.progress("WARN: Upload of audited FPR to SSC failed: %s", e.getMessage());
            }
        }

        ObjectNode result = AviatorSSCAuditHelper.buildResultNode(av, artifactId, action);
        AviatorSSCAuditHelper.setAuditStats(result, auditResult);
        return result;
    }

    private Path downloadFpr(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger) throws IOException {
        logger.progress("Status: Downloading FPR from SSC for app version: %s:%s (id: %s)", av.getApplicationName(), av.getVersionName(), av.getVersionId());

        String prefix = String.format("aviator_%s_%s_", av.getApplicationName().replaceAll("[^a-zA-Z0-9.-]", "_"), av.getVersionName().replaceAll("[^a-zA-Z0-9.-]", "_"));
        Path tempFpr = Files.createTempFile(prefix, ".fpr");

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    tempFpr.toFile(),
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                    progressWriter);
            return tempFpr;
        } catch (UnexpectedHttpResponseException e) {
            Files.deleteIfExists(tempFpr);
            if (e.getStatus() == 400) {
                logger.progress("Audit skipped - no FPR available to audit in SSC for app version %s:%s.", av.getApplicationName(), av.getVersionName());
                LOG.info("SSC returned HTTP 400 when downloading FPR for app version id {}. Assuming no FPR is available.", av.getVersionId());
                return null;
            }
            throw e;
        }
    }

    @SneakyThrows
    private String uploadAuditedFprToSSC(UnirestInstance unirest, File auditedFpr, SSCAppVersionDescriptor av) {
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            JsonNode uploadResponse = SSCFileTransferHelper.restUpload(unirest, SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()), auditedFpr, JsonNode.class, progressWriter);
            return uploadResponse.path("data").path("id").asText("UPLOAD_FAILED");
        }
    }

    @Override
    public String getActionCommandResult() {
        return "AUDITED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
