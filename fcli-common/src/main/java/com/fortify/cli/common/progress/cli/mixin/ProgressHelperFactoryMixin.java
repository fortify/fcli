package com.fortify.cli.common.progress.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.common.progress.helper.ProgressHelperI18n;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class ProgressHelperFactoryMixin {
    @Mixin private CommandHelperMixin commandHelper;
    @Option(names="--no-progress", negatable = true, defaultValue = "false") private boolean noProgress;
    
    public final IProgressHelperI18n createProgressHelper() {
        return new ProgressHelperI18n(commandHelper.getMessageResolver(), noProgress);
    }
}
