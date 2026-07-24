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

import java.util.List;

import com.fortify.cli.aviator._common.remediations_cache.IRemediationsFprSource;
import com.fortify.cli.aviator._common.util.AviatorTempFprFile;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Online SSC remediations source: downloads each artifact FPR to a managed temp path
 * for the duration of {@link EntryAction#accept}, then deletes it.
 *
 * <p>{@link #close()} is a no-op: temps are owned per entry inside {@link #forEachEntry}.
 * The type implements {@link AutoCloseable} so callers can use one try-with-resources
 * pattern for all {@link IRemediationsFprSource} implementations.
 */
public final class SSCOnlineRemediationsFprSource implements IRemediationsFprSource {
    private final UnirestInstance unirest;
    private final IAviatorLogger logger;
    private final IProgressWriter progressWriter;
    private final List<SSCArtifactDescriptor> artifacts;

    public SSCOnlineRemediationsFprSource(
            UnirestInstance unirest,
            IAviatorLogger logger,
            IProgressWriter progressWriter,
            List<SSCArtifactDescriptor> artifacts) {
        FcliSimpleException.throwIf(artifacts == null || artifacts.isEmpty(),
                "No SSC artifacts to apply remediations from");
        this.unirest = unirest;
        this.logger = logger;
        this.progressWriter = progressWriter;
        this.artifacts = List.copyOf(artifacts);
    }

    @Override
    public void forEachEntry(EntryAction action) {
        int total = artifacts.size();
        for (int i = 0; i < total; i++) {
            SSCArtifactDescriptor artifact = artifacts.get(i);
            String id = artifact.getId();
            String label = "artifact id=" + id;
            try (AviatorTempFprFile tempFpr = AviatorTempFprFile.create(id)) {
                logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + id + ")");
                SSCFileTransferHelper.download(
                        unirest,
                        SSCUrls.DOWNLOAD_ARTIFACT(id, true),
                        tempFpr.path(),
                        SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                        progressWriter);
                if (!action.accept(tempFpr.path(), label, id, i + 1, total)) {
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        // Per-entry temps are closed in forEachEntry; nothing retained on this instance.
    }
}
