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
package com.fortify.cli.tool.fcli.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunShellOrJavaCommand;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;

@Command(name = "run")
public class ToolFcliRunCommand extends AbstractToolRunShellOrJavaCommand {
    @Getter private String toolName = ToolFcliCommands.TOOL_NAME;

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var binPath = descriptor.getBinPath();
        var fcliBatPath = binPath.resolve("fcli.bat");
        var fcliExePath = binPath.resolve("fcli.exe");
        var fcliPath = binPath.resolve("fcli");
        Path resultPath;
        if ( ToolPlatformHelper.isWindows() && Files.exists(fcliBatPath) ) {
            resultPath = fcliBatPath;
        } else if ( Files.exists(fcliExePath) ) {
            resultPath = fcliExePath;
        } else {
            resultPath = fcliPath;
        }
        return List.of(resultPath.toString());
    }
    
    @Override
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of("FCLI_JAVA_HOME", "JAVA_HOME");
    }
    
    @Override @SneakyThrows
    protected String getJar(ToolInstallationDescriptor descriptor) {
        var fcliJarPath = descriptor.getInstallPath().resolve("fcli.jar");
        return Files.exists(fcliJarPath) ? fcliJarPath.toString() : null;
    }
}
