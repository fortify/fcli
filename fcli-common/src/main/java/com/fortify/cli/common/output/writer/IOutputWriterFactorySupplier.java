package com.fortify.cli.common.output.writer;

import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;

public interface IOutputWriterFactorySupplier {
    IOutputWriterFactory getOutputWriterFactory();
}
