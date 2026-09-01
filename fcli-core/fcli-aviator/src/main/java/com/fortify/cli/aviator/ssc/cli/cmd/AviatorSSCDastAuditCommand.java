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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.audit.DastAuditFPR;
import com.fortify.cli.aviator.audit.DastAuditFprResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.DastAuditStreamConfig;
import com.fortify.cli.aviator.grpc.DastAuditStreamProcessor;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCFprTransferHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagValidator;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.ResourceUtil;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionRefreshOptions;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.system_state.helper.SSCJobDescriptor;
import com.fortify.cli.ssc.system_state.helper.SSCJobHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "audit-dast")
public class AviatorSSCDastAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCDastAuditCommand.class);

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private AviatorUserSessionDescriptorSupplier sessionDescriptorSupplier;
    @Mixin private SSCAppVersionRefreshOptions refreshOptions;
    @Option(names = {"--app"}) private String appName;
    @Option(names = {"--tag-mapping"}) private String tagMapping;

    private String actionResult = "SKIPPED";

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        Path downloadedFpr = null;
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            var logger = new AviatorLoggerImpl(progressWriter);
            var appVersion = appVersionResolver.getAppVersionDescriptor(unirest);
            var session = sessionDescriptorSupplier.getSessionDescriptor();
            TagMappingConfig tagMappingConfig = loadTagMappingConfig();

            refreshMetricsIfNeeded(unirest, appVersion, logger);

            downloadedFpr = AviatorSSCFprTransferHelper.downloadCurrentStateFpr(
                unirest, appVersion, logger, progressWriter);

            DastAuditFprResult result = auditFpr(
                downloadedFpr, appVersion, session, logger, tagMappingConfig);
            actionResult = result.status().name();
            String artifactId = null;
            if (result.updatedFile() != null && result.succeeded() > 0) {
                validateSSCTagsBeforeUpload(unirest, appVersion, logger, tagMappingConfig);
                logger.progress("Status: Uploading audited DAST FPR to SSC");
                artifactId = AviatorSSCFprTransferHelper.uploadDastFpr(
                    unirest, appVersion, downloadedFpr, progressWriter);
            }
            return buildOutput(appVersion, result, artifactId);
        } catch (RuntimeException e) {
            actionResult = "FAILED";
            throw e;
        } catch (Exception e) {
            actionResult = "FAILED";
            throw new FcliTechnicalException("DAST audit failed", e);
        } finally {
            if (downloadedFpr != null) {
                try {
                    Files.deleteIfExists(downloadedFpr);
                } catch (Exception e) {
                    LOG.warn("Failed to delete temporary DAST FPR {}", downloadedFpr, e);
                }
            }
        }
    }

    private void refreshMetricsIfNeeded(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            AviatorLoggerImpl logger) {
        if (refreshOptions.isRefresh() && appVersion.isRefreshRequired()) {
            logger.progress("Status: Metrics for application version %s:%s are out of date, starting refresh...",
                appVersion.getApplicationName(), appVersion.getVersionName());
            SSCJobDescriptor refreshJob = SSCAppVersionHelper.refreshMetrics(unirest, appVersion);
            if (refreshJob != null) {
                SSCJobHelper.waitForJob(unirest, refreshJob, refreshOptions.getRefreshTimeout());
                logger.progress("Status: Metrics refreshed successfully.");
            }
        }
    }

    private DastAuditFprResult auditFpr(
            Path fprPath,
            SSCAppVersionDescriptor appVersion,
            AviatorUserSessionDescriptor session,
            IAviatorLogger logger,
            TagMappingConfig tagMappingConfig) throws Exception {
        String effectiveAppName = appName != null ? appName : appVersion.getApplicationName();
        var config = DastAuditStreamConfig.builder()
            .token(session.getAviatorToken())
            .applicationName(effectiveAppName)
            .sscApplicationName(appVersion.getApplicationName())
            .sscApplicationVersion(appVersion.getVersionName())
            .build();
        try (var grpcClient = AviatorGrpcClientHelper.createClient(session.getAviatorUrl(), logger, 30);
             var streamProcessor = new DastAuditStreamProcessor(
                 logger, grpcClient.getDastAuditAsyncStub(), grpcClient.getPingScheduler(),
                 grpcClient.getPingIntervalSeconds());
             var fprHandle = new FprHandle(fprPath)) {
            long timeout = Math.max(grpcClient.getDefaultTimeoutSeconds(), 300);
            return DastAuditFPR.audit(fprHandle, config, tagMappingConfig, (streamConfig, workItems, totalReported) ->
                streamProcessor.process(streamConfig, workItems, totalReported)
                    .orTimeout(timeout, TimeUnit.SECONDS));
        } catch (CompletionException e) {
            throw e.getCause() instanceof Exception exception ? exception : e;
        }
    }

    private ObjectNode buildOutput(
            SSCAppVersionDescriptor appVersion,
            DastAuditFprResult audit,
            String artifactId) {
        ObjectNode result = AviatorSSCAuditHelper.buildResultNode(appVersion, artifactId, audit.status().name());
        AviatorSSCAuditHelper.setDastAuditStats(result, audit);
        return result;
    }

    private TagMappingConfig loadTagMappingConfig() {
        TagMappingConfig tagMappingConfig = tagMapping == null || tagMapping.isBlank()
            ? AviatorConfigManager.getInstance().getDefaultDastTagMappingConfig()
            : ResourceUtil.loadYamlFile(new File(tagMapping), TagMappingConfig.class);
        return tagMappingConfig.resolveForDast();
    }

    private void validateSSCTagsBeforeUpload(UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion, IAviatorLogger logger,
            TagMappingConfig tagMappingConfig) {
        List<String> warnings = AviatorSSCTagValidator.validatePreUpload(
            unirest, appVersion.getVersionId(), tagMappingConfig.getTag_id(),
            tagMappingConfig.getMappedValues(), logger);
        LOG.info("DAST tag validation complete. {} warning(s) found.", warnings.size());
    }

    @Override
    public String getActionCommandResult() {
        return actionResult;
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}