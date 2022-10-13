package com.fortify.cli.common.output.writer.output.standard;

import picocli.CommandLine.ITypeConverter;

public final class OutputStoreConfigConverter implements ITypeConverter<OutputStoreConfig> {
    @Override
    public OutputStoreConfig convert(String value) throws Exception {
        int pos = value.indexOf('=');
        String variableName = pos==-1 ? value : value.substring(0, pos);
        String options = pos==-1 ? null : value.substring(pos+1);
        return new OutputStoreConfig(variableName, options);
    }
}