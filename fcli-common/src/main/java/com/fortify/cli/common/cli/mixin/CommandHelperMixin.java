package com.fortify.cli.common.cli.mixin;

import com.fortify.cli.common.output.writer.CommandSpecMessageResolver;
import com.fortify.cli.common.output.writer.IMessageResolver;

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
    public final <T> T getCommandAs(Class<T> asType) {
        return getAs(getCommand(), asType);
    }

    /**
     * Utility method for retrieving the command instance.
     * @return
     */
    public final Object getCommand() {
        return commandSpec.userObject();
    }

    /**
     * Utility method for getting the given object as the given type,
     * returning null if the given object is not an instance of the
     * given type.
     * 
     * TODO This is potentially a reusable method; consider moving elsewhere.
     * @param <T>
     * @param obj
     * @param asType
     * @return
     */
    @SuppressWarnings("unchecked")
    public final static <T> T getAs(Object obj, Class<T> asType) {
        if ( obj!=null && asType.isAssignableFrom(obj.getClass()) ) {
            return (T)obj;
        }
        return null;
    }
}