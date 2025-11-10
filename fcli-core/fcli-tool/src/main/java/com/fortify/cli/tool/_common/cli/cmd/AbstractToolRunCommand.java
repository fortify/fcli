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

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.tool._common.helper.ToolInstallationDescriptor;
import com.fortify.cli.tool.definitions.helper.ToolDefinitionsHelper;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * This abstract base class allows for running a tool that was installed through the
 * corresponding 'install' command. Subclasses can provide the base command to run
 * by overriding the {@link #getBaseCommand(ToolInstallationDescriptor)} method.
 * Alternatively, is there are multiple ways to run the tool (for example shell
 * script or direct java call), subclasses can override the {@link #getBaseCommands(ToolInstallationDescriptor)}
 * method to provide one or more alternative/fallback base commands; if the first
 * base command fails, the second command will be tried, and so on. For the
 * example above (invocation through either shell script or direct invocation),
 * it's recommended to extend from either {@link AbstractToolRunOptionalShellCommand}
 * or {@link AbstractToolRunShellOrJavaCommand}.
 *
 * @author Ruud Senden
 */
public abstract class AbstractToolRunCommand extends AbstractRunnableCommand {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractToolRunCommand.class);
    @Option(names={"-v", "--version"}, required = false, descriptionKey="fcli.tool.run.version")
    private String versionToRun;
    @Option(names={"-d", "--workdir"}, required = false, descriptionKey="fcli.tool.run.workdir")
    private String workDir = System.getProperty("user.dir");
    @Parameters(descriptionKey="fcli.tool.run.tool-args")
    @Getter private List<String> toolArgs;
    
    @Override
    public final Integer call() throws Exception {
        var descriptor = getToolInstallationDescriptor();
        var baseCommands = new ArrayList<>(getBaseCommands(descriptor));
        while (true) {
            try {
                return call(baseCommands.get(0));
            } catch ( Exception e ) {
                if ( baseCommands.size()==1) { throw e; } // No more base commands
                LOG.debug("Command execution failed; trying fallback command");
                baseCommands.remove(0);
            }
        }
    }
    
    private final Integer call(List<String> baseCmd) throws Exception {
        if ( baseCmd==null ) { throw new FcliBugException("Base command to execute may not be null"); }
        var fullCmd = Stream.of(baseCmd, getToolArgs())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
        LOG.debug("Attempting to run "+fullCmd);
        var pb = new ProcessBuilder()
                .command(fullCmd)
                .directory(new File(workDir))
                // .inheritIO(); 
                // Can't use inheritIO as this as it may inherit original stdout/stderr, rather than
                // those created by OutputHelper.OutputType (for example through FcliCommandExecutor).
                // Instead, we use pipes and manually copy the output to current System.out/System.err.
                // This fixes for example https://github.com/fortify/fcli/issues/859.
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE);
        updateProcessBuilder(pb);
        var process = pb.start();
        inheritIO(process.getInputStream(), System.out);
        inheritIO(process.getErrorStream(), System.err);
        try {
            process.waitFor();
        } catch ( InterruptedException ie ) {
            // Best-effort kill if job cancellation interrupted this thread
            try { process.destroy(); } catch ( Exception ignore ) {}
            try { process.destroyForcibly(); } catch ( Exception ignore ) {}
            Thread.currentThread().interrupt();
            throw ie; // Propagate so upstream job manager can mark cancelled
        }
        return process.exitValue();
    }
    
    private static void inheritIO(final InputStream src, final PrintStream dest) {
        new Thread(new Runnable() {
            @SneakyThrows
            public void run() {
                IOUtils.copy(src, dest);
            }
        }).start();
    }

    private final ToolInstallationDescriptor getToolInstallationDescriptor() {
        var toolName = getToolName();
        if ( StringUtils.isBlank(versionToRun) ) { 
            return checkNotNull(ToolInstallationDescriptor.loadLastModified(toolName), "No tool installations detected");
        } else {
            var versionDescriptor = ToolDefinitionsHelper.getToolDefinitionRootDescriptor(toolName).getVersion(versionToRun);
            return checkNotNull(ToolInstallationDescriptor.load(toolName, versionDescriptor), "No tool installation detected for version "+versionToRun);
        }
    }

    private ToolInstallationDescriptor checkNotNull(ToolInstallationDescriptor descriptor, String msg) {
        if ( descriptor==null ) {
            throw new FcliSimpleException(msg);
        }
        return descriptor;
    }
    
    protected abstract String getToolName();
    protected List<List<String>> getBaseCommands(ToolInstallationDescriptor descriptor) {
        return List.of(getBaseCommand(descriptor));
    }
    protected List<String> getBaseCommand(ToolInstallationDescriptor descriptor) {
        return null;
    }
    protected void updateProcessBuilder(ProcessBuilder pb) {};
    
}
