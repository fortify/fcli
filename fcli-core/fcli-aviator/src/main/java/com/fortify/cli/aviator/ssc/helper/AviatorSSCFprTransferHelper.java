/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.aviator.ssc.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Shared SSC transfer operations for Aviator FPR workflows.
 */
public final class AviatorSSCFprTransferHelper {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCFprTransferHelper.class);

    private AviatorSSCFprTransferHelper() {}

    public static Path downloadCurrentStateFpr(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            IAviatorLogger logger,
            IProgressWriter progressWriter) throws IOException {
        return downloadCurrentStateFpr(unirest, appVersion, logger, progressWriter, true);
    }

    public static Path downloadCurrentStateFpr(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            IAviatorLogger logger,
            IProgressWriter progressWriter,
            boolean includeSource) throws IOException {
        logger.progress("Status: Downloading current FPR state from SSC for app version %s:%s (id=%s)",
            appVersion.getApplicationName(), appVersion.getVersionName(), appVersion.getVersionId());
        return downloadFpr(unirest, "aviator_" + appVersion.getVersionId() + "_",
            SSCUrls.DOWNLOAD_CURRENT_FPR(appVersion.getVersionId(), includeSource), progressWriter);
    }

    public static Path downloadArtifactFpr(
            UnirestInstance unirest,
            SSCArtifactDescriptor artifact,
            IAviatorLogger logger,
            IProgressWriter progressWriter) throws IOException {
        logger.progress("Status: Downloading FPR from SSC (artifact id=%s)", artifact.getId());
        return downloadFpr(unirest, "aviator_" + artifact.getId() + "_",
            SSCUrls.DOWNLOAD_ARTIFACT(artifact.getId(), true), progressWriter);
    }

    private static Path downloadFpr(
            UnirestInstance unirest,
            String filePrefix,
            String downloadUrl,
            IProgressWriter progressWriter) throws IOException {
        Path fprPath = Files.createTempFile(filePrefix, ".fpr");
        try {
            SSCFileTransferHelper.download(
                unirest,
                downloadUrl,
                fprPath.toFile(),
                SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                progressWriter);
            return fprPath;
        } catch (RuntimeException e) {
            try {
                Files.deleteIfExists(fprPath);
            } catch (IOException cleanupException) {
                e.addSuppressed(cleanupException);
            }
            throw e;
        }
    }

    public static String uploadFpr(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            Path fprPath,
            IProgressWriter progressWriter) {
        JsonNode uploadResponse = SSCFileTransferHelper.restUpload(
            unirest,
            SSCUrls.PROJECT_VERSION_ARTIFACTS(appVersion.getVersionId()),
            fprPath.toFile(),
            JsonNode.class,
            progressWriter);
        return getUploadedArtifactId(uploadResponse);
    }

    /**
     * An upload is confirmed only when SSC returns an artifact ID. An unconfirmed
     * upload may still have been accepted by SSC.
     */
    public record UploadResult(String artifactId, String failureMessage) {
        public UploadResult {
            if ((artifactId == null) == (failureMessage == null)
                    || (artifactId != null && artifactId.isBlank())
                    || (failureMessage != null && failureMessage.isBlank())) {
                throw new FcliBugException("Upload result must contain either a non-blank artifact ID or a non-blank failure message, not both");
            }
        }
    }

    public static UploadResult uploadEngineSpecificFprBestEffort(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            Path fprPath,
            AviatorSSCFprFilterHelper.Engine engine,
            IProgressWriter progressWriter,
            IAviatorLogger logger) {
        return uploadEngineSpecificFprBestEffort(
            fprPath,
            engine,
            logger,
            candidate -> AviatorSSCFprFilterHelper.filterEngineSpecificFprInPlace(candidate, engine),
            candidate -> uploadFpr(unirest, appVersion, candidate, progressWriter));
    }

    static UploadResult uploadEngineSpecificFprBestEffort(
            Path fprPath,
            AviatorSSCFprFilterHelper.Engine engine,
            IAviatorLogger logger,
            Consumer<Path> pruneFpr,
            Function<Path, String> uploadFpr) {
        Path candidate = null;
        Path fprToUpload = fprPath;
        try {
            candidate = Files.createTempFile(fprPath.toAbsolutePath().getParent(), "aviator_", ".fpr");
            Files.copy(fprPath, candidate, StandardCopyOption.REPLACE_EXISTING);
            pruneFpr.accept(candidate);
            fprToUpload = candidate;
        } catch (Exception e) {
            LOG.warn("WARN: Failed to prune {} FPR for SSC upload; uploading the full original FPR including all engines",
                engine, e);
            logger.progress("WARN: Could not prune %s FPR; uploading the full original FPR including all engines: %s",
                engine, getErrorMessage(e).replaceAll("\\R", " "));
        }

        try {
            String artifactId = uploadFpr.apply(fprToUpload);
            validateArtifactId(artifactId);
            return new UploadResult(artifactId, null);
        } catch (Exception e) {
            String message = getUploadFailureMessage(e);
            String warning = getUploadMessage("warning", engine, message);
            LOG.warn(warning, e);
            logger.progress("%s", warning.replaceAll("\\R", " "));
            return new UploadResult(null, message);
        } finally {
            if (candidate != null) {
                try {
                    Files.deleteIfExists(candidate);
                } catch (Exception e) {
                    LOG.warn("WARN: Failed to delete temporary FPR candidate {}", candidate, e);
                    logger.progress("WARN: Failed to delete temporary FPR candidate: %s",
                        getErrorMessage(e).replaceAll("\\R", " "));
                }
            }
        }
    }

    private static String getErrorMessage(Throwable e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String getUploadFailureMessage(Exception e) {
        String outcome = getUploadMessage("unconfirmed");
        if (e instanceof UnexpectedHttpResponseException response && !response.isParsingFailure()) {
            int status = response.getStatus();
            if (status >= 400 && status < 500 && status != 408) {
                outcome = getUploadMessage("rejected", status);
            }
        }
        String cause = getErrorMessage(e);
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause != null && !cause.equals(getErrorMessage(rootCause))) {
            cause = getUploadMessage("root-cause", cause, getErrorMessage(rootCause));
        }
        return getUploadMessage("failure", outcome, cause);
    }

    private static String getUploadMessage(String keySuffix, Object... args) {
        var messages = ResourceBundle.getBundle("com.fortify.cli.aviator.i18n.AviatorMessages");
        return String.format(messages.getString("fcli.aviator.ssc.audit.upload." + keySuffix), args);
    }

    static String getUploadedArtifactId(JsonNode uploadResponse) {
        String artifactId = uploadResponse == null
            ? null : uploadResponse.path("data").path("id").asText(null);
        validateArtifactId(artifactId);
        return artifactId;
    }

    private static void validateArtifactId(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            throw new FcliTechnicalException(getUploadMessage("missing-artifact-id"));
        }
    }
}
