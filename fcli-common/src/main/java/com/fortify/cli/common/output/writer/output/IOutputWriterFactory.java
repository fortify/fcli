package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;

import picocli.CommandLine.Model.CommandSpec;

public interface IOutputWriterFactory {
    IOutputWriter createOutputWriter(CommandSpec mixee, OutputConfig outputConfig);
}
