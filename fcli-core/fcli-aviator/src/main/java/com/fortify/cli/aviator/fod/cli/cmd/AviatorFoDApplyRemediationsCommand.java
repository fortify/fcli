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
package com.fortify.cli.aviator.fod.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fod.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class AviatorFoDApplyRemediationsCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorFoDApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, description = "Absolute path for source code") private String sourceCodeDirectory;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            FoDReleaseDescriptor rd = releaseResolver.getReleaseDescriptor(unirest);
            return processFprRemediations(unirest, rd, logger);
        }
    }

    @SneakyThrows
    private JsonNode processFprRemediations(UnirestInstance unirest, FoDReleaseDescriptor rd, AviatorLoggerImpl logger) {
        Path downloadedFprPath = null;
        try {
            logger.progress("Status: Downloading Audited FPR from FOD");
            downloadedFprPath = downloadFprFromFod(unirest, rd);

            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            try (FprHandle fprHandle = new FprHandle(downloadedFprPath)) {
                var remediationMetric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger);
                String status = remediationMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
                return AviatorFoDApplyRemediationsHelper.buildResultNode(rd, remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), status);
            }
        } finally {
            if (downloadedFprPath != null) {
                try {
                    Files.deleteIfExists(downloadedFprPath);
                } catch (IndexOutOfBoundsException e) {
                    LOG.warn("WARN: Failed to delete temporary downloaded FPR file: {}", downloadedFprPath, e);
                }
            }
        }
    }

    @SneakyThrows
    private Path  downloadFprFromFod(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        Path fprPath = Files.createTempFile("aviator_" + releaseDescriptor.getReleaseId() + "_", ".fpr");
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                getScanType(), false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        var file = fprPath.toString();
        GetRequest request = getDownloadRequest(unirest, releaseDescriptor, scanDescriptor);
        int status = 202;
        while ( status==202 ) {
            status = request
                    .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return fprPath;
    }



    protected FoDScanType getScanType() {
        return FoDScanType.Static;
    }

    protected GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .accept("application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
    }

    @Override
    public boolean isSingular() {
        return false;
    }

    @Override
    public IOutputHelper getOutputHelper() {
        return null;
    }

    @Override
    public String getActionCommandResult() {
        return "Remediation-Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return null;
    }
}
