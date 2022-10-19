package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.rest.wait.StandardWaitHelperProgressMonitor;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

@Command @ReflectiveAccess
public class StandardWaitHelperProgressMonitorMixin {
    private IMessageResolver messageResolver;
    
    @Spec(Target.MIXEE)
    public void setCommandSpec(CommandSpec commandSpec) {
        commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.messageResolver = new CommandSpecMessageResolver(commandSpec);
    }
    
    public StandardWaitHelperProgressMonitor createProgressMonitor(boolean writeFinalStatus) {
        return new StandardWaitHelperProgressMonitor(messageResolver, writeFinalStatus);
    }
}
