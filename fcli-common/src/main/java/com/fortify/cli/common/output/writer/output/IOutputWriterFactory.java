package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;

public interface IOutputWriterFactory {
    IOutputWriter createOutputWriter(StandardOutputConfig standardOutputConfig);
}
