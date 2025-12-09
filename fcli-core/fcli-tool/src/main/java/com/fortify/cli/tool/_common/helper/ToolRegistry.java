/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.tool._common.helper;
import java.util.Set;

public class ToolRegistry {
    private static final Set<String> REGISTERED_TOOLS = registerTools();
    // TODO Merge this with Tool enum? How about 'jre' which is not in Tool enum?

    public static Set<String> getRegisteredToolNames() {
        return REGISTERED_TOOLS;
    }

    private static Set<String> registerTools() {
        return Set.of("bugtracker-utility", "debricked-cli", "fcli", "fod-uploader", "jre", "sc-client", "vuln-exporter");
    }
}
