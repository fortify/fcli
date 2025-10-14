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
package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.artifact.cli.mixin.SSCArtifactResolverMixin;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractSSCJsonNodeOutputCommand  implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    //Downloading of the FPR will be based on artifact and not app version
    @Mixin private SSCArtifactResolverMixin.RequiredOption artifactResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}) private String sourceCodeDirectory;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCArtifactDescriptor ad = artifactResolver.getArtifactDescriptor(unirest);
            return processFprRemediations(unirest, ad, logger);
        }
    }

    @SneakyThrows
    private JsonNode processFprRemediations(UnirestInstance unirest, SSCArtifactDescriptor ad, AviatorLoggerImpl logger) {
        Path fprPath = Files.createTempFile("aviator_" + ad.getId() + "_", ".fpr");
        try {
            logger.progress("Status: Downloading Audited FPR from SSC");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
                    fprPath.toFile(),
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");

            try (FprHandle fprHandle = new FprHandle(fprPath)) {
                var remediationMetric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger);
                String status = remediationMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";

                return AviatorSSCApplyRemediationsHelper.buildResultNode(ad, remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), status);
            }

        } finally {
            try {
                Files.deleteIfExists(fprPath);
            } catch (IOException e) {
                LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprPath, e);
            }
        }
    }

    @Override
    public boolean isSingular() {return true;}

    @Override
    public String getActionCommandResult() {
        return "Remediations Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
