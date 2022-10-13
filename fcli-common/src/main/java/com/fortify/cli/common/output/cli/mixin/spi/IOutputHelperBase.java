package com.fortify.cli.common.output.cli.mixin.spi;

import com.fortify.cli.common.output.spi.IBasicOutputConfigSupplier;
import com.fortify.cli.common.output.spi.IOutputWriterFactorySupplier;

import picocli.CommandLine.Model.CommandSpec;

public interface IOutputHelperBase extends IBasicOutputConfigSupplier, IOutputWriterFactorySupplier {
    CommandSpec getCommandSpec();
    <T> T getCommandAs(Class<T> asType);
}