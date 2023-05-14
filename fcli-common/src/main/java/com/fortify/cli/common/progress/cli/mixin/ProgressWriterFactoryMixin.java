package com.fortify.cli.common.progress.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterTypeConverter.ProgressWriterTypeIterable;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterI18n;
import com.fortify.cli.common.progress.helper.ProgressWriterType;

import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class ProgressWriterFactoryMixin {
    @Mixin private CommandHelperMixin commandHelper;
    @Getter @Option(names="--progress", defaultValue = "auto", completionCandidates = ProgressWriterTypeIterable.class, converter = ProgressWriterTypeConverter.class ) 
    private ProgressWriterType type;
    
    public final IProgressWriterI18n create() {
        return new ProgressWriterI18n(type, commandHelper.getMessageResolver());
    }
}
