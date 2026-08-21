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
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Handles FPR download and upload operations against SSC for the correlate-sast-dast command.
 */
public final class AviatorSSCCorrelateDownloadHelper {

    private AviatorSSCCorrelateDownloadHelper() {}

    /**
     * Downloads the FPR artifact for a single artifact ID. Used for DAST FPR download
     * (individual artifact is needed so its webinspect.xml can be enriched and re-uploaded).
     */
    public static Path downloadArtifactFpr(UnirestInstance unirest, SSCArtifactDescriptor ad,
                                            AviatorLoggerImpl logger, IProgressWriter progressWriter) throws IOException {
        return AviatorSSCFprTransferHelper.downloadArtifactFpr(unirest, ad, logger, progressWriter);
    }

    /**
     * Downloads the current merged SAST FPR for an application version.
     * This merged FPR is safe to re-upload to SSC after adding audit tags because its
     * internal FVDL contains only audit state (no raw scan results), which SSC processes
     * as an audit-only update instead of a new scan submission.
     * Using DOWNLOAD_ARTIFACT for the SAST FPR and re-uploading it causes SSC to treat
     * it as a duplicate scan and puts the artifact into an error state.
     */
    public static Path downloadCurrentSastFpr(UnirestInstance unirest, SSCAppVersionDescriptor av,
                                               AviatorLoggerImpl logger, IProgressWriter progressWriter) throws IOException {
        Path fprPath = Files.createTempFile("aviator_sast_merged_", ".fpr");
        logger.progress("Status: Downloading current merged SAST FPR from SSC for %s:%s",
            av.getApplicationName(), av.getVersionName());
        SSCFileTransferHelper.download(
            unirest,
            SSCUrls.DOWNLOAD_CURRENT_FPR(av.getVersionId(), false),
            fprPath.toFile(),
            SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
            progressWriter);
        return fprPath;
    }

    /**
     * Uploads an enriched DAST FPR to SSC and returns the new artifact ID.
     */
    public static String uploadEnrichedDastFpr(UnirestInstance unirest, SSCAppVersionDescriptor av,
                                              Path enrichedDastFpr, IProgressWriter progressWriter) {
        return AviatorSSCFprTransferHelper.uploadDastFpr(unirest, av, enrichedDastFpr, progressWriter);
    }

    /**
     * Uploads an enriched SAST FPR (with updated DAST_CORRELATION_STATUS tags) to SSC.
     * Uses the REST artifacts endpoint — same as the audit command — so SSC merges only
     * the audit.xml changes without treating the upload as a new scan result.
     * Using UPLOAD_RESULT_FILE would cause SSC to reject it as a duplicate scan (error state).
     */
    public static void uploadEnrichedSastFpr(UnirestInstance unirest, SSCAppVersionDescriptor av,
                                              Path enrichedSastFpr, IProgressWriter progressWriter) {
        SSCFileTransferHelper.restUpload(
            unirest,
            SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getVersionId()),
            enrichedSastFpr.toFile(),
            JsonNode.class,
            progressWriter
        );
    }
}
