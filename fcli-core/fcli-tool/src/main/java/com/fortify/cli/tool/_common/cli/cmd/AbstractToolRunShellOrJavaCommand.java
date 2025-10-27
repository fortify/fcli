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
package com.fortify.cli.tool._common.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolPlatformHelper;

import picocli.CommandLine.Option;

public abstract class AbstractToolRunShellOrJavaCommand extends AbstractToolRunCommand {
    @Option(names={"--use-shell"}, required = false, defaultValue="auto", descriptionKey="fcli.tool.run.java.use-shell")
    private UseShell useShell;
    
    @Override
    protected final List<List<String>> getBaseCommands(ToolInstallationDescriptor descriptor) {
        // If the tool install command supports both java and native platforms, jarName may
        // be null if native platform binaries were installed.
        var jarName = getJar(descriptor); 
        if ( useShell==UseShell.yes || jarName==null ) {
            // Only run base command if useShell==yes or native binaries were installed
            return List.of(getBaseCommand(descriptor));
        } else if ( useShell==UseShell.no ) {
            // Only run Java command if useShell==no
            return List.of(getJavaBaseCommand(descriptor));
        } else {
            // If useShell==auto, first try running base command (usually shell script),
            // use Java command as fallback if base command invocation fails.
            return List.of(getBaseCommand(descriptor), getJavaBaseCommand(descriptor));
        }
    }
    
    public enum UseShell {
        no, yes, auto
    }
    
    protected List<String> getJavaBaseCommand(ToolInstallationDescriptor descriptor) {
        return List.of(getJavaCommand(descriptor), "-jar", getJar(descriptor));
    }
    
    private final String getJavaCommand(ToolInstallationDescriptor descriptor) {
        var baseJavaCmd = ToolPlatformHelper.isWindows() ? "java.exe" : "java";
        var embeddedJavaCmdPath = descriptor.getInstallPath().resolve("jre/bin").resolve(baseJavaCmd);
        if ( Files.exists(embeddedJavaCmdPath) ) { // Look for java command in embedded JRE
            return embeddedJavaCmdPath.toString();
        } else {
            for ( var javaHomeEnvVarName : getJavaHomeEnvVarNames() ) { // Look for java command in env vars
                var javaHome = EnvHelper.env(javaHomeEnvVarName);
                var javaCmdPathFromEnv = javaHome==null ? null :  Path.of(javaHome, "bin", baseJavaCmd);
                if ( javaCmdPathFromEnv!=null && Files.exists(javaCmdPathFromEnv) ) {
                    return javaCmdPathFromEnv.toString();
                }
            }
        }
        return "java"; // Fallback to use java command from path
    }
    
    protected List<String> getJavaHomeEnvVarNames() {
        return List.of(getToolName().toUpperCase().replace('-', '_')+"JAVA_HOME", "JAVA_HOME");
    }
    protected abstract List<String> getBaseCommand(ToolInstallationDescriptor descriptor);
    protected abstract String getJar(ToolInstallationDescriptor descriptor);
}
