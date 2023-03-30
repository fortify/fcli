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