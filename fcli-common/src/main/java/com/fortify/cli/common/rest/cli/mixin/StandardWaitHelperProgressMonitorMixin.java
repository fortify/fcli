package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.rest.wait.StandardWaitHelperProgressMonitor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command
public class StandardWaitHelperProgressMonitorMixin {
    @Mixin private CommandHelperMixin commandHelper;
    @Option(names="--no-progress") private boolean noProgress;
    
    public StandardWaitHelperProgressMonitor createProgressMonitor(boolean writeFinalStatus) {
        return noProgress 
                ? null 
                : new StandardWaitHelperProgressMonitor(commandHelper.getMessageResolver(), writeFinalStatus);
    }
}
