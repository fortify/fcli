package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.ProgressWriterType;
import com.fortify.cli.common.rest.wait.StandardWaitHelperProgressMonitor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command
public class StandardWaitHelperProgressMonitorMixin {
    @Mixin private CommandHelperMixin commandHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    public StandardWaitHelperProgressMonitor create(boolean writeFinalStatus) {
        return progressWriterFactory.getType()==ProgressWriterType.none 
                ? null 
                : new StandardWaitHelperProgressMonitor(progressWriterFactory.create(), commandHelper.getMessageResolver(), writeFinalStatus);
    }
}
