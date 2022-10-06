package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputConfig;

public interface IOutputWriterFactory {
    IOutputWriter createOutputWriter(OutputConfig outputConfig);
}
