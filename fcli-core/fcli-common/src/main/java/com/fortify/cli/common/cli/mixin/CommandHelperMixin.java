/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.cli.mixin;

import java.util.Optional;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.util.JavaHelper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

@Command
public final class CommandHelperMixin implements ICommandAware, ICommandHelper {
    private CommandSpec commandSpec;
    private IMessageResolver messageResolver;
    private CommandLine rootCommandLine;
    
    @Override
    public final void setCommandSpec(CommandSpec commandSpec) {
        this.commandSpec = commandSpec;
        this.messageResolver = new CommandSpecMessageResolver(commandSpec);
        this.rootCommandLine = _getRootCommandLine(commandSpec);
    }

    /**
     * Utility method for retrieving the command being invoked as the given 
     * type, returning null if the command is not an instance of the given 
     * type.
     */
    public final <T> Optional<T> getCommandAs(Class<T> asType) {
        return JavaHelper.as(getCommand(), asType);
    }

    /**
     * Utility method for retrieving the command instance.
     * @return
     */
    public final Object getCommand() {
        ensureCommandSpecInjected();
        return commandSpec.userObject();
    }

    public final CommandSpec getCommandSpec() {
        ensureCommandSpecInjected();
        return commandSpec;
    }

    public final IMessageResolver getMessageResolver() {
        ensureCommandSpecInjected();
        return messageResolver;
    }

    public final CommandLine getRootCommandLine() {
        ensureCommandSpecInjected();
        return rootCommandLine;
    }
    
    public final CommandLine _getRootCommandLine(CommandSpec spec) {
        var cl = spec.commandLine();
        while ( true ) {
            if ( cl.getParent()==null ) break;
            cl = cl.getParent();
        }
        return cl;
    }

    private void ensureCommandSpecInjected() {
        if ( commandSpec==null ) {
            throw new FcliBugException("""
                    CommandSpec hasn't been injected into CommandHelperMixin. 
                    \tCommon causes:
                    \t- CommandHelperMixin field hasn't been annotated with @Mixin
                    \t- Another class in the class hierarchy defines a mixin with same field name:
                    \t  - Mixin field names must be unique in the class hierarchy due to picocli flaw
                    \t    (mixins are returned as a Map<String, Mixin> where the key is the field name,
                    \t    so mixins with same field name will overwrite each other).
                    \t  - Runnable commands should use AbstractRunnableCommand::getCommandHelper rather
                    \t    than declaring their own CommandHelperMixin field to avoid this issue.
                    """);
        }
    }
}