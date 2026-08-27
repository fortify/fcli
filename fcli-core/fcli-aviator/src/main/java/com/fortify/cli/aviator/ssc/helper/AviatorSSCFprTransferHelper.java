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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Shared SSC transfer operations for artifact-specific DAST FPR workflows.
 */
public final class AviatorSSCFprTransferHelper {
    private AviatorSSCFprTransferHelper() {}

    public static Path downloadArtifactFpr(
            UnirestInstance unirest,
            SSCArtifactDescriptor artifact,
            IAviatorLogger logger,
            IProgressWriter progressWriter) throws IOException {
        Path fprPath = Files.createTempFile("aviator_" + artifact.getId() + "_", ".fpr");
        try {
            logger.progress("Status: Downloading FPR from SSC (artifact id=%s)", artifact.getId());
            SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_ARTIFACT(artifact.getId(), true),
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

    public static String uploadDastFpr(
            UnirestInstance unirest,
            SSCAppVersionDescriptor appVersion,
            Path dastFpr,
            IProgressWriter progressWriter) {
        JsonNode uploadResponse = SSCFileTransferHelper.restUpload(
            unirest,
            SSCUrls.PROJECT_VERSION_ARTIFACTS(appVersion.getVersionId()),
            dastFpr.toFile(),
            JsonNode.class,
            progressWriter);
        return getUploadedArtifactId(uploadResponse);
    }

    static String getUploadedArtifactId(JsonNode uploadResponse) {
        String artifactId = uploadResponse == null
            ? null : uploadResponse.path("data").path("id").asText(null);
        if (artifactId == null || artifactId.isBlank()) {
            throw new FcliTechnicalException("SSC DAST FPR upload response did not contain an artifact ID");
        }
        return artifactId;
    }
}