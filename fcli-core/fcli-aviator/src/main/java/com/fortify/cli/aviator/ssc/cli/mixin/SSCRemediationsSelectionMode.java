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
package com.fortify.cli.aviator.ssc.cli.mixin;

/**
 * SSC remediations online selection mode. Wire value is stored in cache manifest
 * {@code selection.mode} and must remain stable for existing/future caches.
 */
public enum SSCRemediationsSelectionMode {
    ARTIFACT_ID("artifact-id"),
    LATEST("latest"),
    ALL("all");

    private final String wireValue;

    SSCRemediationsSelectionMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /** Value written to remediations cache manifest selection metadata. */
    public String wireValue() {
        return wireValue;
    }
}
