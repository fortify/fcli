/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.cli.mixin;

import java.util.Optional;

import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.util.JavaHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;

@Command
public final class CommandHelperMixin implements ICommandAware {
    @Getter private CommandSpec commandSpec;
    @Getter private IMessageResolver messageResolver;
    
    @Override
    public final void setCommandSpec(CommandSpec commandSpec) {
        this.commandSpec = commandSpec;
        this.messageResolver = new CommandSpecMessageResolver(commandSpec);
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
        return commandSpec.userObject();
    }
}