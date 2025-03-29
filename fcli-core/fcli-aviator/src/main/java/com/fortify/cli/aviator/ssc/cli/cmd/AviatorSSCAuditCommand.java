package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.core.AuditFPR;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "audit")
@DefaultVariablePropertyName("artifactId")
public class AviatorSSCAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private AviatorUserSessionDescriptorSupplier sessionDescriptorSupplier;
    @Option(names = {"--app"}, required = false) private String appName;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAuditCommand.class);

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);

            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);
            File fprFile = File.createTempFile("aviator_" + av.getApplicationName() + "_" + av.getVersionName(), ".fpr");

            try {
                progressWriter.writeProgress("Status: Downloading FPR from SSC");
                SSCFileTransferHelper.download(
                        unirest,
                        SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                        fprFile,
                        SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

                progressWriter.writeProgress("Status: Processing FPR with Aviator");
                File processedFileResult = AuditFPR.auditFPR(fprFile, sessionDescriptor.getAviatorToken(), sessionDescriptor.getAviatorUrl(), appName, logger);

                if (processedFileResult != null) {
                    progressWriter.writeProgress("Status: Uploading FPR to SSC");
                    JsonNode uploadResponse = uploadFpr(unirest, processedFileResult, av);
                    JsonNode dataNode = uploadResponse.path("data");
                    String id = dataNode.path("id").asText("");
                    return av.asObjectNode().put("artifactId", id);
                } else {
                    progressWriter.writeProgress("No issues to audit, skipping upload");
                    return av.asObjectNode().put("artifactId", "N/A").put("action", "SKIPPED");
                }
            } finally {
                if (fprFile.exists()) {
                    if (!fprFile.delete()) {
                        LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    @SneakyThrows
    private JsonNode uploadFpr(UnirestInstance unirest, File file, SSCAppVersionDescriptor av) {
        return SSCFileTransferHelper.upload(unirest, SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()), file,
                SSCFileTransferHelper.ISSCAddUploadTokenFunction.QUERYSTRING_MAT, JsonNode.class);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return SSCAppVersionHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}