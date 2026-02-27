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
package com.fortify.cli.tool.env.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolInstallationsResolver.ToolInstallationRecord;

/**
 * Contextual metadata for a single resolved tool installation, used as input to
 * environment-output generators.
 */
public record ToolEnvContext(
        Tool tool,
        ToolInstallationRecord record,
        ToolInstallationDescriptor descriptor,
        ObjectNode model) {

    public String installDir() {
        return descriptor.getInstallDir();
    }

    public String binDir() {
        return descriptor.getBinDir();
    }

    public String globalBinDir() {
        return descriptor.getGlobalBinDir();
    }

    public String version() {
        return record.versionDescriptor().getVersion();
    }

    public String envPrefix() {
        return tool.getDefaultEnvPrefix();
    }

    public boolean isDefault() {
        return record.isDefault();
    }

    public String cmd() {
        var cmdNode = model.get("cmd");
        return cmdNode == null || cmdNode.isNull() ? null : cmdNode.asText();
    }
}
