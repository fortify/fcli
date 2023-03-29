package com.fortify.cli.common.output.writer;

import com.fortify.cli.common.util.PicocliSpecHelper;

import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Model.CommandSpec;

@RequiredArgsConstructor
public final class CommandSpecMessageResolver implements IMessageResolver {
    private final CommandSpec commandSpec;
    
    @Override
    public String getMessageString(String keySuffix) {
        return PicocliSpecHelper.getMessageString(commandSpec, keySuffix);
    }
}
