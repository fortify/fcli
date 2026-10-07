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
package com.fortify.cli.aviator._common.remediations_cache;

import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheReader.ResolvedFpr;
import com.fortify.cli.common.exception.FcliBugException;

/**
 * Offline remediations source: ordered FPR paths from a remediations cache zip.
 * Product identity (and thus artifact vs release id) comes from the validated entry model.
 */
public final class CacheRemediationsFprSource implements IRemediationsFprSource {
    private final RemediationsCacheReader reader;

    private CacheRemediationsFprSource(RemediationsCacheReader reader) {
        this.reader = reader;
    }

    public static CacheRemediationsFprSource open(Path cacheZip, String expectedProduct) {
        RemediationsCacheReader reader = RemediationsCacheReader.open(cacheZip);
        try {
            reader.requireProduct(expectedProduct);
            return new CacheRemediationsFprSource(reader);
        } catch (RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    public RemediationsCacheReader reader() {
        return reader;
    }

    @Override
    public void forEachEntry(EntryAction action) {
        List<ResolvedFpr> units = reader.getOrderedResolvedFprs();
        int total = units.size();
        for (int i = 0; i < total; i++) {
            ResolvedFpr unit = units.get(i);
            RemediationsCacheEntry entry = unit.entry();
            String label = entry.getPath() != null
                    ? entry.getPath()
                    : unit.fprPath().getFileName().toString();
            if (!action.accept(unit.fprPath(), label, productId(entry), i + 1, total)) {
                break;
            }
        }
    }

    /**
     * Id for progress/result lists: artifactId (SSC) or releaseId (FoD).
     * Entries are validated before resolve, so exactly one product block is present.
     */
    private static String productId(RemediationsCacheEntry entry) {
        if (entry.getSscData() != null) {
            return entry.getSscData().getArtifactId();
        }
        if (entry.getFodData() != null) {
            return entry.getFodData().getReleaseId();
        }
        throw new FcliBugException(
                "Remediations cache entry missing product data after validation: " + entry.getPath());
    }

    @Override
    public void close() {
        reader.close();
    }
}
