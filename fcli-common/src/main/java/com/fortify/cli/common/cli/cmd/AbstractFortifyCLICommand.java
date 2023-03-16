package com.fortify.cli.common.cli.cmd;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class AbstractFortifyCLICommand {
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
