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
package com.fortify.cli.tool.sc_client.cli.cmd;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.DebugHelper;
import com.fortify.cli.common.util.PlatformHelper;
import com.fortify.cli.tool._common.cli.cmd.AbstractToolRunShellOrJavaCommand;
import com.fortify.cli.tool._common.helper.Tool;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool._common.helper.ToolJreResolver;

import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "run")
public class ToolSCClientRunCommand extends AbstractToolRunShellOrJavaCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ToolSCClientRunCommand.class);
    @Option(names="--logdir", required=false)
    private Path logDir;
    private String detectedJavaHome;
    
    @Override
    protected final Tool getTool() {
        return Tool.SC_CLIENT;
    }
    
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
        var ext = PlatformHelper.isWindows() ? ".bat" : "";
        return List.of(descriptor.getBinPath().resolve("scancentral"+ext).toString());
    }
    
    @Override
    protected List<String> getJavaBaseCommand(ToolInstallationDescriptor descriptor) {
        LOG.debug("Resolving Java command for direct invocation");
        
        // Use generalized JRE resolver
        var resolveConfig = ToolJreResolver.JreResolveConfig.builder()
            .descriptor(descriptor)
            .envVarPrefixes(new String[]{"SC_CLIENT", "SCANCENTRAL"})
            .compatibleVersions(new String[]{"21", "17", "11", "8"})
            .javaExecutableName(PlatformHelper.isWindows() ? "java.exe" : "java")
            .includeGenericJavaHome(true)
            .build();
        
        var resolveResult = ToolJreResolver.resolveJavaCommand(resolveConfig);
        detectedJavaHome = resolveResult.getJavaHome();
        
        // Validate Java executable exists and is accessible
        String javaCommand = resolveResult.getJavaCommand();
        if (!javaCommand.equals("java") && !javaCommand.equals("java.exe")) {
            // Not falling back to PATH - validate the specific executable
            Path javaExec = Path.of(javaCommand);
            if (!Files.exists(javaExec)) {
                throw new FcliSimpleException(String.format(
                    "Java executable not found at '%s'. " +
                    "JRE source: %s. " +
                    "Please reinstall the tool with --with-jre option or ensure the correct Java environment variable is set.",
                    javaCommand,
                    descriptor.getJreSource()
                ));
            }
            if (!Files.isRegularFile(javaExec)) {
                throw new FcliSimpleException(String.format(
                    "Java executable at '%s' is not a regular file. " +
                    "Please reinstall the tool with --with-jre option.",
                    javaCommand
                ));
            }
            LOG.debug("Validated Java executable: {}", javaCommand);
        }
        
        var cmd = new ArrayList<String>();
        cmd.add(javaCommand);
        if ( logDir!=null ) {
            cmd.add("-Dlog4j.dir="+logDir.toAbsolutePath().normalize().toString());
        }
        cmd.add("-jar");
        cmd.add(getJar(descriptor));
        return cmd;
    }
    
    @Override
    protected void updateProcessBuilder(ProcessBuilder pb) {
        if ( logDir!=null ) {
            pb.environment().put("SCANCENTRAL_LOG", logDir.toAbsolutePath().normalize().toString());
        }
        // Set SCANCENTRAL_JAVA_HOME if we detected a Java home
        if ( detectedJavaHome!=null ) {
            LOG.debug("Setting SCANCENTRAL_JAVA_HOME={}", detectedJavaHome);
            System.out.println("SCANCENTRAL_JAVA_HOME: "+detectedJavaHome);
            pb.environment().put("SCANCENTRAL_JAVA_HOME", detectedJavaHome);
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
