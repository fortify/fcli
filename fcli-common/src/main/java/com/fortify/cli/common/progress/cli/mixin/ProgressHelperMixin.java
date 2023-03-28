package com.fortify.cli.common.progress.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.progress.helper.AbstractI18nProgressHelperWrapper;
import com.fortify.cli.common.progress.helper.IProgressHelper;
import com.fortify.cli.common.progress.helper.ProgressHelperFactory;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class ProgressHelperMixin extends AbstractI18nProgressHelperWrapper {
    @Mixin private CommandHelperMixin commandHelper;
    @Option(names="--no-progress", negatable = true, defaultValue = "false") private boolean noProgress;
    
    @Override
    protected IMessageResolver createMessageResolver() {
        return commandHelper.getMessageResolver();
    }
    
    @Override
    protected IProgressHelper createProgressHelper() {
        return ProgressHelperFactory.createProgressHelper(noProgress);
    }
}
