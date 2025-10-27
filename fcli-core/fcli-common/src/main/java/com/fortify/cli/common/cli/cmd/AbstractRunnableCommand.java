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
package com.fortify.cli.common.cli.cmd;

import java.io.File;
import java.util.concurrent.Callable;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.log.LogMaskLevel;
import com.fortify.cli.common.mcp.MCPExclude;

import ch.qos.logback.classic.Level;
import lombok.AccessLevel;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * This abstract class should be used as the base class for all runnable fcli commands.
 * It is responsible for providing the following fcli features:
 * <ul>
 *  <li>Providing standard command configuration settings (default value provider, ...)</li>
 *  <li>Providing standard command options (--help, --log-level, ...)</li>
 *  <li>Injecting {@link CommandSpec} representing the current command into
 *      any mixins that implement the {@link ICommandAware} interface</li>
 * </ul>
 *
 * @author Ruud Senden
 */
public abstract class AbstractRunnableCommand implements Callable<Integer> {
    // Have picocli inject the CommandSpec representing the current command
    @Spec private CommandSpec commandSpec;
    @Getter(AccessLevel.PROTECTED) @Mixin private CommandHelperMixin commandHelper;
    
    // ArgGroup for generic options like --help
    @ArgGroup(exclusive = false, headingKey = "fcli.genericOptions.heading", order = 50) 
    @Getter private GenericOptionsArgGroup genericOptions = new GenericOptionsArgGroup();
    
    /**
     * Enum defining available log levels
     */
    public static enum LogLevel {
        TRACE(Level.TRACE),
        DEBUG(Level.DEBUG),
        INFO(Level.INFO),
        WARN(Level.WARN),
        ERROR(Level.ERROR),
        NONE(null);

        @Getter private final Level logbackLevel;
        LogLevel(Level logbackLevel) {
            this.logbackLevel = logbackLevel;
        }
    }

    /**
     * This class (used as an {@link ArgGroup}) defines common fcli options that 
     * are available on every fcli command.
     * 
     * We {@link MCPExclude} all generic options, as these options only
     * take effect on top-level fcli invocation, not fcli invocations performed
     * through {@link FcliCommandExecutorFactory} as we do for MCP tools. We also
     * ignore the help option; if it's useful to have usage help returned by MCP
     * tools, it's probably better to define a separate usageHelp(cmd) tool, rather
     * than having a --help option on every individual tool.
     */
    public static final class GenericOptionsArgGroup {
        @Option(names = {"-h", "--help"}, usageHelp = true) @MCPExclude
        private boolean usageHelpRequested;
        
        @Option(names = "--env-prefix", defaultValue = "FCLI_DEFAULT", paramLabel = "<prefix>") @MCPExclude
        @Getter private String envPrefix;
        
        @Option(names = "--log-file") @MCPExclude
        @Getter private File logFile;
        
        @Option(names = "--log-level") @MCPExclude
        @Getter private LogLevel logLevel;
        
        @Option(names = "--log-mask", defaultValue = "medium", paramLabel = "<level>") @MCPExclude
        @Getter private LogMaskLevel logMaskLevel;
        
        @Option(names = "--debug") @MCPExclude
        @Getter private boolean debug;
    }
}
