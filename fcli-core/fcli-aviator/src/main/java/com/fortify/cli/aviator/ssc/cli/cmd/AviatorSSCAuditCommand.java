package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.File;
import java.io.IOException;

import com.fortify.cli.aviator.ssc.cli.helper.AviatorSSCAuditHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator.audit.AuditFPR;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
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
        File downloadedFpr = null;
        try {
            downloadedFpr = downloadFprFromSSC(unirest, av, logger);

            if (downloadedFpr == null) {
                return AviatorSSCAuditHelper.buildResultNode(av, "N/A", "SKIPPED (no FPR available to audit)");
            }

            FPRAuditResult auditResult = performAviatorAudit(downloadedFpr, token, url, av, logger);
            String action = AviatorSSCAuditHelper.getDetailedAction(auditResult);
            String progressMessage = AviatorSSCAuditHelper.getProgressMessage(auditResult);
            logger.progress(progressMessage);

            String artifactId = "UPLOAD_SKIPPED";
            if (auditResult.getUpdatedFile() != null && !"SKIPPED".equals(auditResult.getStatus()) && !"FAILED".equals(auditResult.getStatus())) {
                artifactId = uploadAuditedFprToSSC(unirest, auditResult.getUpdatedFile(), av);
            }

            return AviatorSSCAuditHelper.buildResultNode(av, artifactId, action);

        } finally {
            if (downloadedFpr != null && downloadedFpr.exists() && !downloadedFpr.delete()) {
                LOG.warn("WARN: Failed to delete temporary downloaded FPR file: {}", downloadedFpr.getAbsolutePath());
            }
        }
    }

    private File downloadFprFromSSC(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger) throws IOException {
        logger.progress("Status: Downloading FPR from SSC for app version: %s:%s (id: %s)", av.getApplicationName(), av.getVersionName(), av.getVersionId());
        File fprFile = File.createTempFile("aviator_" + av.getApplicationName() + "_" + av.getVersionName(), ".fpr");
        try {
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    fprFile,
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
            return fprFile;
        } catch (UnexpectedHttpResponseException e) {
            if (e.getStatus() == 400) {
                logger.progress("Audit skipped - no FPR available to audit in SSC for app version %s:%s.", av.getApplicationName(), av.getVersionName());
                LOG.info("SSC returned HTTP 400 when downloading FPR for app version id {}. Assuming no FPR is available.", av.getVersionId());
                if (fprFile.exists()) fprFile.delete();
                return null;
            }
            throw e;
        }
    }

    private FPRAuditResult performAviatorAudit(File fprFile, String token, String url, SSCAppVersionDescriptor av, AviatorLoggerImpl logger) {
        logger.progress("Status: Processing FPR with Aviator");
        return AuditFPR.auditFPR(fprFile, token, url, appName, av.getApplicationName(), av.getVersionName(), logger, tagMapping);
    }

    @SneakyThrows
    private String uploadAuditedFprToSSC(UnirestInstance unirest, File auditedFpr, SSCAppVersionDescriptor av) {
        JsonNode uploadResponse = SSCFileTransferHelper.restUpload(unirest, SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()), auditedFpr, JsonNode.class);
        return uploadResponse.path("data").path("id").asText("UPLOAD_FAILED");
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
        return "AUDITED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}