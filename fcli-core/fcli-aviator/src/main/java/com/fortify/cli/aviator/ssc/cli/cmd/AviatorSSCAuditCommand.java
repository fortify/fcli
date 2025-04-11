package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.core.AuditFPR;
import com.fortify.cli.aviator.core.model.FPRAuditResult;
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
    @Option(names = {"--tag-mapping"}, required = false, description = "Tag Mapping") private String tagMapping;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAuditCommand.class);
    private String auditAction;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);
            return processFpr(unirest, av, sessionDescriptor.getAviatorToken(), sessionDescriptor.getAviatorUrl(), logger);
        }
    }

    @SneakyThrows
    private JsonNode processFpr(UnirestInstance unirest, SSCAppVersionDescriptor av, String token, String url, AviatorLoggerImpl logger) {
        File fprFile = File.createTempFile("aviator_" + av.getApplicationName() + "_" + av.getVersionName(), ".fpr");
        try {
            logger.progress("Status: Downloading FPR from SSC");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);

            logger.progress("Status: Processing FPR with Aviator");
            FPRAuditResult auditResult = AuditFPR.auditFPR(fprFile, token, url, appName, logger, tagMapping);
            auditAction = getDetailedAction(auditResult);

            String artifactId = "UPLOAD_SKIPPED";
            String progressMessage = getProgressMessage(auditResult);

            if (auditResult.getUpdatedFile() != null && !"SKIPPED".equals(auditResult.getStatus()) && !"FAILED".equals(auditResult.getStatus())) {
                logger.progress(progressMessage);
                JsonNode uploadResponse = uploadFpr(unirest, auditResult.getUpdatedFile(), av);
                artifactId = uploadResponse.path("data").path("id").asText("");
            } else {
                logger.progress(progressMessage);
            }

            ObjectNode result = av.asObjectNode();
            result.put("id", av.getVersionId());
            result.put("applicationName", av.getApplicationName());
            result.put("name", av.getVersionName());
            result.put("artifactId", artifactId);
            result.put("action", auditAction);
            return result;
        } finally {
            if (fprFile.exists() && !fprFile.delete()) {
                LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprFile.getAbsolutePath());
            }
        }
    }

    private String getDetailedAction(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                String reason = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown reason";
                return "SKIPPED (" + reason + ")";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "FAILED (" + message + ")";
            case "PARTIALLY_AUDITED":
                return String.format("PARTIALLY_AUDITED (%d/%d audited)",
                        auditResult.getIssuesSuccessfullyAudited(),
                        auditResult.getTotalIssuesToAudit());
            case "AUDITED":
                return "AUDITED";
            default:
                return "UNKNOWN";
        }
    }

    private String getProgressMessage(FPRAuditResult auditResult) {
        switch (auditResult.getStatus()) {
            case "SKIPPED":
                return "No issues to audit, skipping upload";
            case "FAILED":
                String message = auditResult.getMessage() != null ? auditResult.getMessage() : "Unknown error";
                return "Audit failed: " + message;
            case "PARTIALLY_AUDITED", "AUDITED":
                return auditResult.getUpdatedFile() != null
                        ? "Status: Uploading FPR to SSC"
                        : "Audit completed but no file updated";
            default:
                return "Unknown audit status";
        }
    }

    @SneakyThrows
    private JsonNode uploadFpr(UnirestInstance unirest, File file, SSCAppVersionDescriptor av) {
        return SSCFileTransferHelper.upload(unirest, SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()), file,
                SSCFileTransferHelper.ISSCAddUploadTokenFunction.QUERYSTRING_MAT, JsonNode.class);
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        ObjectNode transformed = record.deepCopy();
        if (transformed.has("versionId")) {
            transformed.set("Id", transformed.remove("versionId"));
        }
        if (transformed.has("applicationName")) {
            transformed.set("Application name", transformed.remove("applicationName"));
        }
        if (transformed.has("versionName")) {
            transformed.set("Name", transformed.remove("versionName"));
        }
        return SSCAppVersionHelper.renameFields(transformed);
    }

    @Override
    public String getActionCommandResult() {
        return auditAction != null ? auditAction : "FAILED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}