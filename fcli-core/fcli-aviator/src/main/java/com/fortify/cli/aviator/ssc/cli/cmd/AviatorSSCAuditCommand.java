package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAuditHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.ssc.issue.cli.mixin.SSCIssueFilterSetOptionMixin;
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
    @Mixin private SSCIssueFilterSetOptionMixin filterSetOptions;
    @Option(names = {"--app"}) private String appName;
    @Option(names = {"--tag-mapping"}) private String tagMapping;
    @Option(names = {"--no-filterset"}) private boolean noFilterSet;
    @Option(names = {"--folder"}, split = ",") @DisableTest(DisableTest.TestType.MULTI_OPT_PLURAL_NAME) private List<String> folderNames;
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCAuditCommand.class);

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        Path downloadedFprPath = null;
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);

            long auditableIssueCount = AviatorSSCAuditHelper.getAuditableIssueCount(unirest, av, logger, noFilterSet, filterSetOptions, folderNames);
            if (auditableIssueCount == 0) {
                logger.progress("Audit skipped - no auditable issues found matching the specified filters.");
                return AviatorSSCAuditHelper.buildResultNode(av, "N/A", "SKIPPED (no auditable issues)");
            }

            downloadedFprPath = downloadFpr(unirest, av, logger);
            if (downloadedFprPath == null) {
                return AviatorSSCAuditHelper.buildResultNode(av, "N/A", "SKIPPED (no FPR available to audit)");
            }

            return processFpr(unirest, av, sessionDescriptor.getAviatorToken(), sessionDescriptor.getAviatorUrl(), logger, downloadedFprPath);
        } finally {
            if (downloadedFprPath != null) {
                Files.deleteIfExists(downloadedFprPath);
            }
        }
    }

    @SneakyThrows
    private JsonNode processFpr(UnirestInstance unirest, SSCAppVersionDescriptor av, String token, String url, AviatorLoggerImpl logger, Path downloadedFprPath) {
        FPRAuditResult auditResult;

        try (FprHandle fprHandle = new FprHandle(downloadedFprPath)) {
            auditResult = AuditFPR.auditFPR(AuditFprOptions.builder()
                    .fprHandle(fprHandle).token(token).url(url)
                    .appVersion(appName)
                    .sscAppName(av.getApplicationName())
                    .sscAppVersion(av.getVersionName())
                    .logger(logger)
                    .tagMappingPath(tagMapping)
                    .filterSetNameOrId(filterSetOptions.getFilterSetTitleOrId())
                    .noFilterSet(noFilterSet)
                    .folderNames(folderNames)
                    .build());
        }

        String action = AviatorSSCAuditHelper.getDetailedAction(auditResult);
        logger.progress(AviatorSSCAuditHelper.getProgressMessage(auditResult));

        String artifactId = "UPLOAD_SKIPPED";
        if (auditResult.getUpdatedFile() != null && !"SKIPPED".equals(auditResult.getStatus()) && !"FAILED".equals(auditResult.getStatus())) {
            artifactId = uploadAuditedFprToSSC(unirest, auditResult.getUpdatedFile(), av);
        }

        return AviatorSSCAuditHelper.buildResultNode(av, artifactId, action);
    }

    private Path downloadFpr(UnirestInstance unirest, SSCAppVersionDescriptor av, AviatorLoggerImpl logger) throws IOException {
        logger.progress("Status: Downloading FPR from SSC for app version: %s:%s (id: %s)", av.getApplicationName(), av.getVersionName(), av.getVersionId());

        String prefix = String.format("aviator_%s_%s_", av.getApplicationName().replaceAll("[^a-zA-Z0-9.-]", "_"), av.getVersionName().replaceAll("[^a-zA-Z0-9.-]", "_"));
        Path tempFpr = Files.createTempFile(prefix, ".fpr");

        try {
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), true),
                    tempFpr.toFile(),
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN);
            return tempFpr;
        } catch (UnexpectedHttpResponseException e) {
            Files.deleteIfExists(tempFpr);
            if (e.getStatus() == 400) {
                logger.progress("Audit skipped - no FPR available to audit in SSC for app version %s:%s.", av.getApplicationName(), av.getVersionName());
                LOG.info("SSC returned HTTP 400 when downloading FPR for app version id {}. Assuming no FPR is available.", av.getVersionId());
                return null;
            }
            throw e;
        }
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