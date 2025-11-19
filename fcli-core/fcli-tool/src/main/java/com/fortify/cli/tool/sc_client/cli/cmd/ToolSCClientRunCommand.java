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
package com.fortify.cli.tool.sc_client.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunShellOrJavaCommand;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "run")
public class ToolSCClientRunCommand extends AbstractToolRunShellOrJavaCommand {
    @Option(names="--logdir", required=false)
    private Path logDir;
    @Getter private String toolName = ToolSCClientCommands.TOOL_NAME;
    
    @Override
    public List<String> getToolArgs() {
        var orgArgs = super.getToolArgs();
        var result = new ArrayList<String>();
        if ( DebugHelper.isDebugEnabled() && (orgArgs==null || !orgArgs.contains("-debug")) ) {
            result.add("-debug");
        }
        if ( orgArgs!=null ) {
            result.addAll(orgArgs);
        }
        return result;
    }

    @Override
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        var ext = ToolPlatformHelper.isWindows() ? ".bat" : "";
        return List.of(descriptor.getBinPath().resolve("scancentral"+ext).toString());
    }
    
    @Override
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of("SCANCENTRAL_JAVA_HOME", "JAVA_HOME");
    }
    
    @Override
    protected List<String> getJavaBaseCommand(ToolInstallationDescriptor descriptor) {
        // Get java command, preferring stored JRE location
        String javaCommand = getJavaCommandForDescriptor(descriptor);
        var cmd = new ArrayList<String>();
        cmd.add(javaCommand);
        if ( logDir!=null ) {
            cmd.add("-Dlog4j.dir="+logDir.toAbsolutePath().normalize().toString());
        }
        cmd.add("-jar");
        cmd.add(getJar(descriptor));
        return cmd;
    }
    
    private String getJavaCommandForDescriptor(ToolInstallationDescriptor descriptor) {
        var baseJavaCmd = ToolPlatformHelper.isWindows() ? "java.exe" : "java";
        
        // First check if JRE was specified during installation
        String storedJreHome = descriptor.getJreHome();
        if (StringUtils.isNotBlank(storedJreHome)) {
            var javaCmdFromStored = Path.of(storedJreHome, "bin", baseJavaCmd);
            if (Files.exists(javaCmdFromStored)) {
                return javaCmdFromStored.toString();
            }
        }
        
        // Check for embedded JRE
        var embeddedJavaCmdPath = descriptor.getInstallPath().resolve("jre/bin").resolve(baseJavaCmd);
        if (Files.exists(embeddedJavaCmdPath)) {
            return embeddedJavaCmdPath.toString();
        }
        
        // Check environment variables
        for (var javaHomeEnvVarName : getJavaHomeEnvVarNames()) {
            var javaHome = EnvHelper.env(javaHomeEnvVarName);
            var javaCmdPathFromEnv = javaHome == null ? null : Path.of(javaHome, "bin", baseJavaCmd);
            if (javaCmdPathFromEnv != null && Files.exists(javaCmdPathFromEnv)) {
                return javaCmdPathFromEnv.toString();
            }
        }
        
        // Fallback to java from PATH
        return "java";
    }
    
    @Override
    protected void updateProcessBuilder(ProcessBuilder pb) {
        if ( logDir!=null ) {
            pb.environment().put("SCANCENTRAL_LOG", logDir.toAbsolutePath().normalize().toString());
        }
    }
    
    @Override @SneakyThrows
    protected String getJar(ToolInstallationDescriptor descriptor) {
        var coreLibPath = descriptor.getInstallPath().resolve("Core/lib");
        return Files.find(coreLibPath, 1, (path, basicFileAttributes) -> path.getFileName().toString().startsWith("scancentral-launcher"))
                .findFirst().orElseThrow(()->new IllegalStateException("Can't find ScanCentral Client launcher jar"))
                .toString();
    }
}
