package com.fortify.cli.common.output.cli.mixin.spi;

import com.fortify.cli.common.output.writer.output.IOutputWriterFactory;

public interface IOutputWriterFactorySupplier {
    IOutputWriterFactory getOutputWriterFactory();
}
