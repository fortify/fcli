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
package com.fortify.cli.util._common.helper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Composite {@link IJobEventListener} that fans out all events to multiple delegates.
 * Useful for combining push notifications with caching.
 *
 * @author Ruud Senden
 */
public final class CompositeJobEventListener implements IJobEventListener {
    private final List<IJobEventListener> delegates;

    public CompositeJobEventListener(IJobEventListener... delegates) {
        this.delegates = List.of(delegates);
    }

    public CompositeJobEventListener(List<IJobEventListener> delegates) {
        this.delegates = new CopyOnWriteArrayList<>(delegates);
    }

    @Override
    public void onJobStarted(String jobId, String description) {
        for (var d : delegates) { d.onJobStarted(jobId, description); }
    }

    @Override
    public void onRecord(String jobId, JsonNode record) {
        for (var d : delegates) { d.onRecord(jobId, record); }
    }

    @Override
    public void onProgress(String jobId, String message) {
        for (var d : delegates) { d.onProgress(jobId, message); }
    }

    @Override
    public void onJobComplete(String jobId, int exitCode, String stderr, String stdout) {
        for (var d : delegates) { d.onJobComplete(jobId, exitCode, stderr, stdout); }
    }
}
