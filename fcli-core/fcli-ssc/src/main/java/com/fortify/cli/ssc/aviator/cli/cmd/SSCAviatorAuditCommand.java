package com.fortify.cli.ssc.aviator.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.core.AuditFPR;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Command(name = "audit")
@DefaultVariablePropertyName("id")
public class SSCAviatorAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    @Option(names = {"-t", "--token"}, required = true) private String token;
    @Option(names = {"--tenant"}, required = true) private String tenantName;
    @Option(names = {"-u", "--url"}, required = true) private String url;
    private static final Logger logger = LoggerFactory.getLogger(SSCAviatorAuditCommand.class);

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        Path tempDir = null;
        File fprFile = null;
        File processedFile = null;

        try (var progressWriter = progressWriterFactory.create()) {
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);
            tempDir = Files.createTempDirectory("ssc_aviator_");

            fprFile = File.createTempFile(String.format("aviator_%s_%s_", av.getApplicationName(), av.getVersionName()), ".fpr", tempDir.toFile());

            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            progressWriter.writeProgress("Status: Processing FPR");
            processedFile = AuditFPR.auditFpr(fprFile, token, tenantName, url);

            progressWriter.writeProgress("Status: Uploading FPR to SSC");
            JsonNode uploadResponse = uploadFpr(unirest, processedFile, av);
            JsonNode dataNode = uploadResponse.get("data");
            String id = dataNode.has("id") ? dataNode.get("id").asText() : "";

            return av.asObjectNode()
                    .put("artifactId", id);
        } catch (Exception e) {
            logger.error("Error during Aviator audit process", e);
            throw e;
        } finally {
            cleanupResources(tempDir);
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

    private void cleanupResources(Path tempDir) {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                                logger.debug("Deleted path: {}", path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete path: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                logger.warn("Failed to clean up temporary directory: {}", tempDir, e);
            }
        }
    }
}