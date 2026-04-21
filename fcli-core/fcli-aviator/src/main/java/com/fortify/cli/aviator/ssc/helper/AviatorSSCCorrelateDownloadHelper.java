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
     * Downloads an FPR artifact from SSC to a temp file and returns the path.
     */
    public static Path downloadArtifactFpr(UnirestInstance unirest, SSCArtifactDescriptor ad,
                                            AviatorLoggerImpl logger, IProgressWriter progressWriter) throws IOException {
        Path fprPath = Files.createTempFile("aviator_" + ad.getId() + "_", ".fpr");
        logger.progress("Status: Downloading FPR from SSC (artifact id=" + ad.getId() + ")");
        SSCFileTransferHelper.download(
            unirest,
            SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
            fprPath.toFile(),
            SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
            progressWriter);
        return fprPath;
    }

    /**
     * Uploads an enriched DAST FPR to SSC using the HTML upload endpoint.
     */
    public static void uploadEnrichedDastFpr(UnirestInstance unirest, SSCAppVersionDescriptor av,
                                              Path enrichedDastFpr, IProgressWriter progressWriter) {
        SSCFileTransferHelper.htmlUpload(
            unirest,
            SSCUrls.UPLOAD_RESULT_FILE(av.getVersionId()),
            enrichedDastFpr.toFile(),
            SSCFileTransferHelper.ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
            String.class,
            progressWriter
        );
    }
}
