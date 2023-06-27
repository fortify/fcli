/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.cli.cmd;

import java.util.Map;

import com.fortify.cli.common.cli.mixin.ICommandAware;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

public class AbstractFortifyCLICommand {
    @Spec private CommandSpec commandSpec;
    private boolean mixinsInitialized = false;
    @ArgGroup(exclusive = false, headingKey = "fcli.genericOptions.heading", order = 50) 
    @Getter private GenericOptionsArgGroup genericOptions = new GenericOptionsArgGroup();
    
    public static enum LogLevel {
        TRACE(Level.TRACE),
        DEBUG(Level.DEBUG),
        INFO(Level.INFO),
        WARN(Level.WARN),
        ERROR(Level.ERROR);

        @Getter private final Level logbackLevel;
        LogLevel(Level logbackLevel) {
            this.logbackLevel = logbackLevel;
        }
    }
    
    protected final void initMixins() {
        if ( !mixinsInitialized ) {
            initMixins(commandSpec, commandSpec.mixins());
            mixinsInitialized = true;
        }
    }
    
    private void initMixins(CommandSpec commandSpec, Map<String, CommandSpec> mixins) {
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

    public static final class GenericOptionsArgGroup {
        @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
        private boolean usageHelpRequested;
        
        @Option(names = "--env-prefix", defaultValue = "FCLI_DEFAULT")
        @Getter private String envPrefix;
        
        @Option(names = "--log-file")
        @Getter private String logFile;
        
        @Option(names = "--log-level")
        @Getter private LogLevel logLevel;
    }
}
