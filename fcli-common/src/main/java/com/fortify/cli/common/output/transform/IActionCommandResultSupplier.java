package com.fortify.cli.common.output.transform;

import com.fortify.cli.common.output.writer.record.AbstractFormattedRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;

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
 * {@link AbstractFormattedRecordWriter} automatically adds __action__ as
 * a field to the output format options if {@link RecordWriterConfig#isAddActionColumn()}
 * is set to true, so no need to manually add this column to (default)
 * output options.  
 * @author rsenden
 *
 */
public interface IActionCommandResultSupplier {
    public static final String actionFieldName = "__action__";
    public String getActionCommandResult();
}
