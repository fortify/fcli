package com.fortify.cli.common.output.writer.output.standard;

public interface IOutputOptions {
    OutputFormatConfig getOutputFormatConfig();
    VariableStoreConfig getVariableStoreConfig();
    String getOutputFile(); 
}
