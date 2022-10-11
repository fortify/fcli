package com.fortify.cli.common.output.cli.mixin.spi.output;

import picocli.CommandLine.Model.CommandSpec;

public interface IOutputHelperBase extends IBasicOutputConfigSupplier, IOutputWriterFactorySupplier {
    CommandSpec getCommandSpec();
    <T> T getCommandAs(Class<T> asType);
}