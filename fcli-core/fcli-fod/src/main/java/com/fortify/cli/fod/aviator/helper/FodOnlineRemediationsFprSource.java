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
package com.fortify.cli.fod.aviator.helper;

import com.fortify.cli.aviator._common.remediations_cache.IRemediationsFprSource;
import com.fortify.cli.aviator._common.util.AviatorTempFprFile;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Online FoD remediations source: downloads the release FPR to a managed temp path
 * for the duration of {@link EntryAction#accept}, then deletes it.
 *
 * <p>{@link #close()} is a no-op: temps are owned per entry inside {@link #forEachEntry}.
 * Implements {@link AutoCloseable} so callers share one try-with-resources pattern with
 * cache sources.
 */
public final class FoDOnlineRemediationsFprSource implements IRemediationsFprSource {
    private final UnirestInstance unirest;
    private final IAviatorLogger logger;
    private final FoDReleaseDescriptor releaseDescriptor;

    public FoDOnlineRemediationsFprSource(
            UnirestInstance unirest,
            IAviatorLogger logger,
            FoDReleaseDescriptor releaseDescriptor) {
        this.unirest = unirest;
        this.logger = logger;
        this.releaseDescriptor = releaseDescriptor;
    }

    @Override
    public void forEachEntry(EntryAction action) {
        String id = releaseDescriptor.getReleaseId();
        String label = "release id=" + id;
        try (AviatorTempFprFile tempFpr = AviatorTempFprFile.create(id)) {
            logger.progress("Status: Downloading Audited FPR from FOD");
            FoDRemediationsFprDownloadHelper.downloadStaticRemediationsFpr(
                    unirest, releaseDescriptor, tempFpr.path());
            if (!action.accept(tempFpr.path(), label, id, 1, 1)) {
                return;
            }
        }
    }

    @Override
    public void close() {
        // Per-entry temps are closed in forEachEntry; nothing retained on this instance.
    }
}
