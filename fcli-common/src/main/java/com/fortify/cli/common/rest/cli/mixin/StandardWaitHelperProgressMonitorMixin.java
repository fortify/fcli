package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.rest.wait.StandardWaitHelperProgressMonitor;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

@Command
public class StandardWaitHelperProgressMonitorMixin {
    private IMessageResolver messageResolver;
    @Option(names="--no-progress") private boolean noProgress;
    
    @Spec(Target.MIXEE)
    public void setCommandSpec(CommandSpec commandSpec) {
        commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        this.messageResolver = new CommandSpecMessageResolver(commandSpec);
    }
    
    public StandardWaitHelperProgressMonitor createProgressMonitor(boolean writeFinalStatus) {
        return noProgress ? null : new StandardWaitHelperProgressMonitor(messageResolver, writeFinalStatus);
    }
}
