package com.fortify.cli.common.progress.cli.mixin;

import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.progress.helper.AbstractI18nProgressHelperWrapper;
import com.fortify.cli.common.progress.helper.IProgressHelper;
import com.fortify.cli.common.progress.helper.ProgressHelperFactory;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Spec.Target;

public class ProgressHelperMixin extends AbstractI18nProgressHelperWrapper {
    @Option(names="--no-progress", negatable = true, defaultValue = "false") private boolean noProgress;
    
    @Spec(Target.MIXEE)
    public void setCommandSpec(CommandSpec commandSpec) {
        commandSpec = commandSpec.commandLine()==null ? commandSpec : commandSpec.commandLine().getCommandSpec();
        setMessageResolver(new CommandSpecMessageResolver(commandSpec));
    }
    
    @Override
    protected IProgressHelper createProgressHelper() {
        return ProgressHelperFactory.createProgressHelper(noProgress);
    }
}
