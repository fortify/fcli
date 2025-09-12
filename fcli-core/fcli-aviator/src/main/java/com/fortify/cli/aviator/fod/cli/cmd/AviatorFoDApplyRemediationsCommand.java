package com.fortify.cli.aviator.fod.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
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
import com.fortify.cli.aviator.fod.helper.AviatorFoDApplyRemediationsHelper;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.StandardCopyOption;

@Command(name = "apply-remediations")
public class AviatorFoDApplyRemediationsCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    //@Mixin private CommonOptionMixins.RequiredFile outputFileMixin;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorFoDApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, required = false, description = "Absolute path for source code") private String sourceCodeDirectory;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            FoDReleaseDescriptor rd = releaseResolver.getReleaseDescriptor(unirest);
            return processFprRemediations(unirest, rd, logger);
        }
    }

    private JsonNode processFprRemediations(UnirestInstance unirest, FoDReleaseDescriptor rd, AviatorLoggerImpl logger) {
        File downloadedFpr = null;
        try {
            logger.progress("Status: Downloading Audited FPR from FOD");
            downloadedFpr = downloadFprFromFod(unirest, rd);
            //if(downloadedFpr==null)
              //  return AviatorFODApplyRemediationHelper.buildResultNode(rd, 0,0,0, "SKIPPED (no FPR available)");
            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            var remediationMetric =  ApplyAutoRemediationOnSource.applyRemediations(downloadedFpr, sourceCodeDirectory, logger);
            String status = remediationMetric.appliedRemediations()>0 ? "Remediation-Applied" : "No-Remediation-Applied";
            return AviatorFoDApplyRemediationsHelper.buildResultNode(rd ,remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), status);

        }finally {
            if(downloadedFpr!=null && downloadedFpr.exists() && !downloadedFpr.delete())
                LOG.warn("WARN: Failed to delete temporary downloaded FPR file: {}", downloadedFpr.getAbsolutePath());

        }
    }

    @SneakyThrows
    private File downloadFprFromFod(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor){
        File fprFile = File.createTempFile("aviator_" + releaseDescriptor.getReleaseId() + "_" , ".fpr");
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                getScanType(), false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        var file = fprFile.getAbsolutePath();
        GetRequest request = getDownloadRequest(unirest, releaseDescriptor, scanDescriptor);
        int status = 202;
        while ( status==202 ) {
            status = request
                    .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return fprFile;
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

