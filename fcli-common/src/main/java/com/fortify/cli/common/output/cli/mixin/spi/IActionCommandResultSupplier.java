package com.fortify.cli.common.output.cli.mixin.spi;

/**
 * This interface can be implemented by commands to return the action
 * that was performed. If provided, this will result in an {@code __action__}
 * property being added to every output record being processed.
 * The value returned by the {@link #getActionCommandResult()} method
 * should usually be in one of the following formats:
 * <ul>
 *  <li>Past tense of the command name in upper case, i.e. "DELETED", if
 *      the remote system performs the requested actions immediately</li>
 *  <li>Command name in upper case followed by "_REQUESTED", i.e. 
 *      "DELETE_REQUESTED", if the remote system performs the requested 
 *      actions in the background, in which case the actions may not have 
 *      been completed yet by the time we output the results.</li>
 * </ul>
 * As most commands will be using table output with a predefined set of 
 * default columns, the {@code __action__} property should be added to
 * the set of default columns for those action commands.
 * @author rsenden
 *
 */
public interface IActionCommandResultSupplier {
    public String getActionCommandResult();
}
