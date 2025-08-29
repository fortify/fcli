package com.fortify.cli.aviator.ssc.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.helper.AviatorSSCApplyRemediationHelper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;

@Command(name = "apply-remediations")
public class AviatorSSCRemediationCommand extends AbstractSSCJsonNodeOutputCommand  implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    //Downloading of the FPR will be based on artifact and not app version
    @Mixin private SSCArtifactResolverMixin.RequiredOption artifactResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCRemediationCommand.class);
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
        File fprFile = File.createTempFile("aviator_" + ad.getId() + "_" , ".fpr");
        //Downloading the Audited FPR
        try {
            logger.progress("Status: Downloading Audited FPR from SSC");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            int[] remediationStatus =  ApplyAutoRemediationOnSource.applyRemediations(fprFile, sourceCodeDirectory, logger);
            String status = remediationStatus[1]>0 ? "Remediation-Applied" : "No-Remediation-Applied";


            return AviatorSSCApplyRemediationHelper.buildResultNode(ad,remediationStatus[0], remediationStatus[1], remediationStatus[0] - remediationStatus[1], status);


        } finally {
            if (fprFile.exists() && !fprFile.delete()) {
                LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprFile.getAbsolutePath());
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
