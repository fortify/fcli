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
package com.fortify.cli.tool.vuln_exporter.cli.cmd;

import java.util.List;

import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunShellOrJavaCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;

import lombok.SneakyThrows;
import picocli.CommandLine.Command;

@Command(name = "run")
public class ToolVulnExporterRunCommand extends AbstractToolRunShellOrJavaCommand {
    
    @Override
    protected final Tool getTool() {
        return Tool.VULN_EXPORTER;
    }

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var ext = PlatformHelper.isWindows() ? ".bat" : "";
        return List.of(descriptor.getBinPath().resolve("FortifyVulnerabilityExporter"+ext).toString());
    }
    
    @Override
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of("FVE_JAVA_HOME", "JAVA_HOME");
    }
    
    @Override @SneakyThrows
    protected String getJar(ToolInstallationDescriptor descriptor) {
        return descriptor.getInstallPath().resolve("FortifyVulnerabilityExporter.jar").toString();
    }
}
