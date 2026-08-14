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

/**
 * Source of audited FPR files for apply-remediations (cache zip or online download).
 * Implementations own any resources (zip FS, temp files) until {@link #close()}.
 *
 * <p>Paths passed to {@link EntryAction#accept} are valid only for the duration of that call
 * (online sources may download to a temp file and delete it afterward).
 */
public interface IRemediationsFprSource extends AutoCloseable {

    /**
     * Invokes {@code action} for each FPR in order. Returning {@code false} from the action
     * stops iteration early (for example when an issue-id filter is exhausted).
     */
    void forEachEntry(EntryAction action);

    @Override
    void close();

    @FunctionalInterface
    interface EntryAction {
        /**
         * @param fprPath host-readable FPR path (zip entry or temp file)
         * @param label   human-readable label for progress/logs
         * @param id      artifact id, release id, or empty
         * @param index   1-based index
         * @param total   total entries
         * @return {@code false} to stop processing further entries
         */
        boolean accept(Path fprPath, String label, String id, int index, int total);
    }
}
