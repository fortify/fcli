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
package com.fortify.cli.common.output.cli.mixin;

import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.common.output.writer.output.IOutputWriter;
import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public class StandardOutputWriterFactoryMixin implements IOutputWriterFactory {
    @Mixin
    private CommandHelperMixin commandHelper;

    @ArgGroup(headingKey = "arggroup.output.heading", exclusive = false, order = 30)
    private OutputOptionsArgGroup outputOptionsArgGroup = new OutputOptionsArgGroup();

    public OutputOptionsArgGroup getOutputOptionsArgGroup() { return outputOptionsArgGroup; }

    @Override
    public IOutputWriter createOutputWriter(StandardOutputConfig defaultOutputConfig) {
        return new StandardOutputWriter(commandHelper.getCommandSpec(), outputOptionsArgGroup, defaultOutputConfig);
    }

    /** Returns the selected RecordWriterFactory based on options or default config without creating writer. */
    public RecordWriterFactory getSelectedRecordWriterFactory(StandardOutputConfig defaultOutputConfig) {
        var outputFormatConfig = outputOptionsArgGroup==null ? null : outputOptionsArgGroup.getOutputFormatConfig();
        var result = outputFormatConfig==null || outputFormatConfig.getRecordWriterFactory()==null
                ? defaultOutputConfig.defaultFormat()
                : outputFormatConfig.getRecordWriterFactory();
        return result==null ? RecordWriterFactory.table : result;
    }
    
    @Override
    public RecordWriterStyle getRecordWriterStyle() {
        return RecordWriterStyle.apply(getOutputOptionsArgGroup().getOutputStyleElements());
    }
}
