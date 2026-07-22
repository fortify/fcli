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

/**
 * Offline remediations source: ordered FPR paths from a remediations cache zip.
 */
public final class CacheRemediationsFprSource implements IRemediationsFprSource {
    public enum IdKind {
        ARTIFACT_ID,
        RELEASE_ID
    }

    private final RemediationsCacheReader reader;
    private final IdKind idKind;

    private CacheRemediationsFprSource(RemediationsCacheReader reader, IdKind idKind) {
        this.reader = reader;
        this.idKind = idKind;
    }

    public static CacheRemediationsFprSource open(Path cacheZip, String expectedProduct, IdKind idKind) {
        RemediationsCacheReader reader = RemediationsCacheReader.open(cacheZip);
        try {
            reader.requireProduct(expectedProduct);
            return new CacheRemediationsFprSource(reader, idKind);
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
            String raw = idKind == IdKind.ARTIFACT_ID ? entry.getArtifactId() : entry.getReleaseId();
            String id = raw != null ? raw : "";
            if (!action.accept(unit.fprPath(), label, id, i + 1, total)) {
                break;
            }
        }
    }

    @Override
    public void close() {
        reader.close();
    }
}
