package com.fortify.cli.ssc.aviator.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.core.AuditFPR;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;

@Command(name = "audit")
@DefaultVariablePropertyName("artifactId")
public class SSCAviatorAuditCommand extends AbstractSSCJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Option(names = {"-t", "--token"}, required = true) private String token;
    @Option(names = {"-u", "--url"}, required = true) private String url;
    private static final Logger LOG = LoggerFactory.getLogger(SSCAviatorAuditCommand.class);

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);

            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);
            File fprFile = File.createTempFile("aviator_" + av.getApplicationName() + "_" + av.getVersionName(), ".fpr");
            fprFile.deleteOnExit();
            progressWriter.writeProgress("Status: Downloading FPR from SSC");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            progressWriter.writeProgress("Status: Processing FPR with Aviator");
            File processedFile = AuditFPR.auditFPR(fprFile, token, url, logger);
            processedFile.deleteOnExit();

            progressWriter.writeProgress("Status: Uploading FPR to SSC");
            JsonNode uploadResponse = uploadFpr(unirest, processedFile, av);
            JsonNode dataNode = uploadResponse.get("data");
            String id = dataNode.has("id") ? dataNode.get("id").asText() : "";

            return av.asObjectNode().put("artifactId", id);
        } catch (AviatorSimpleException e) {
            LOG.error("Aviator audit failed: {}", e.getMessage());
            throw new FcliSimpleException(e.getMessage());
        } catch (AviatorTechnicalException e) {
            LOG.error("Technical error during Aviator audit: {}", e.getMessage(), e);
            throw new FcliTechnicalException("Aviator audit failed due to a technical issue: " + e.getMessage(), e);
        } catch (IOException e) {
            LOG.error("I/O error during audit process: {}", e.getMessage(), e);
            throw new FcliTechnicalException("Failed to process FPR file due to an I/O error.", e);
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