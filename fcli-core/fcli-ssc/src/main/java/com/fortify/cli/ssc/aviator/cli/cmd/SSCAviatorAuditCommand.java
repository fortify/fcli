package com.fortify.cli.ssc.aviator.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.core.AuditFPR;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.SSCUrls;
import com.fortify.cli.ssc._common.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.artifact.cli.mixin.SSCArtifactDownloadOptions;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;


@Command(name = "audit")
public class SSCAviatorAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private SSCArtifactDownloadOptions downloadOptions;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    @Option(names = {"--tenant-token", "--at"}, required = true) private String tenantToken;
    @Option(names = {"--tenant-name", "--tn"}, required = true) private String tenantName;
    @Option(names = {"--aviator-url", "--au"}, required = true) private String aviatorUrl;
    Logger logger = LoggerFactory.getLogger(SSCAviatorAuditCommand.class);

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (var progressWriter = progressWriterFactory.create()) {
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);
            Path tempDir = Files.createTempDirectory("ssc_aviator_");

            File fprFile;
            if (downloadOptions.getDestination() != null && downloadOptions.getDestination().getFile() != null) {
                fprFile = downloadOptions.getDestination().getFile();
            } else {
                fprFile = tempDir.resolve(String.format("%s_%s.fpr", av.getApplicationName(), av.getVersionName())).toFile();
            }
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), downloadOptions.isIncludeSources()),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            progressWriter.writeProgress("Status: Processing FPR");

            var processedFile = AuditFPR.auditFpr(fprFile, tenantToken, tenantName, aviatorUrl);

            progressWriter.writeProgress("Status: Uploading FPR to SSC");
            JsonNode uploadResponse = uploadFpr(unirest, processedFile, av);
            JsonNode dataNode = uploadResponse.get("data");
            String id = dataNode.has("id") ? dataNode.get("id").asText() : "";

            return av.asObjectNode()
                    .put("artifactId", id)
                    .put(IActionCommandResultSupplier.actionFieldName, "UPDATED");
        }
    }

    @SneakyThrows
    private JsonNode uploadFpr(UnirestInstance unirest, File file, SSCAppVersionDescriptor av) {
        return SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()),
                file,
                SSCFileTransferHelper.ISSCAddUploadTokenFunction.QUERYSTRING_MAT,
                JsonNode.class
        );
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

