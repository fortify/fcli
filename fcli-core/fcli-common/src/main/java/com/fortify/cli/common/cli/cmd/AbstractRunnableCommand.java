/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.cli.cmd;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.common.cli.mixin.ICommandAware;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskLevel;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.util.FcliBuildProperties;
import com.fortify.cli.common.util.JavaHelper;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
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
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRunnableCommand.class);
    // Have picocli inject the CommandSpec representing the current command
    @Spec private CommandSpec commandSpec;
    
    // Boolean indicating whether mixins have already been initialized by
    // the initMixins() method
    private boolean initialized = false;
    
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
     * This method is supposed to be invoked by all command implementations
     * in their {@link Runnable#run()} method (after picocli has had a chance
     * to inject our {@link CommandSpec}). It will check whether mixins have
     * already been initialized for this command; if not, the {@link #initMixins(CommandSpec, Map)}
     * method will be invoked to initialize the mixins.
     */
    protected final void initialize() {
        if ( !initialized ) {
            registerLogMasks(commandSpec);
            initMixins(commandSpec, commandSpec.mixins());
            logVersionAndArgs(commandSpec);
            initialized = true;
        }
    }
    
    private static final void logVersionAndArgs(CommandSpec commandSpec) {
        LOG.info("fcli version: {} ", FcliBuildProperties.INSTANCE.getFcliBuildInfo());
        LOG.info("fcli arguments: {} {} ", commandSpec.qualifiedName(), commandSpec.commandLine().getParseResult().expandedArgs());
    }

    private static final void registerLogMasks(CommandSpec commandSpec) {
        for ( var option : commandSpec.options() ) {
            var value = option.getValue();
            if ( value!=null ) {
                JavaHelper.as(option.userObject(), Field.class)
                    .ifPresent(field->registerLogMask(field, value));
            }
        }
    }
    
    private static final void registerLogMask(Field field, Object value) {
        LogMaskHelper.INSTANCE.registerValue(field.getAnnotation(MaskValue.class), LogMaskSource.CLI_OPTION, value);
    }
        

    /**
     * This method recursively iterates over all given mixins to inject our {@link CommandSpec} 
     * into any mixins implementing the {@link ICommandAware} interface.
     */
    private static final void initMixins(CommandSpec commandSpec, Map<String, CommandSpec> mixins) {
        if ( mixins != null ) {
            for ( CommandSpec mixin : mixins.values() ) {
                Object userObject = mixin.userObject();
                if ( userObject!=null && userObject instanceof ICommandAware) {
                    ((ICommandAware)userObject).setCommandSpec(commandSpec);
                }
                initMixins(commandSpec, mixin.mixins());
            }
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
