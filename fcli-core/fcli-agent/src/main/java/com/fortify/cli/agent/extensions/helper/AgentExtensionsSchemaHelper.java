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
package com.fortify.cli.agent.extensions.helper;

import com.fortify.cli.common.util.SemVer;

/**
 * Validates schema version compatibility for extensions-distribution.yaml.
 */
public final class AgentExtensionsSchemaHelper {
    /** The schema version supported by this version of fcli */
    public static final String SUPPORTED_SCHEMA_VERSION = "1.0.0";

    private AgentExtensionsSchemaHelper() {}

    /**
     * Check whether the descriptor's schema version is compatible with
     * this version of fcli.
     * @return true if compatible (same major, fcli minor >= descriptor minor)
     */
    public static boolean isCompatible(String descriptorSchemaVersion) {
        // Normalize: if version is "1.0", treat as "1.0.0"
        var normalized = normalizeVersion(descriptorSchemaVersion);
        var supported = new SemVer(SUPPORTED_SCHEMA_VERSION);
        var descriptor = new SemVer(normalized);
        return supported.isCompatibleWith(descriptor);
    }

    private static String normalizeVersion(String version) {
        if (version == null) { return "0.0.0"; }
        var parts = version.split("\\.");
        if (parts.length == 2) { return version + ".0"; }
        return version;
    }
}
