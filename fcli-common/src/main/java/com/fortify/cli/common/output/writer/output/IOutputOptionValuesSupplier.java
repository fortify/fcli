package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputMixin.OutputFormatConfig;

public interface IOutputOptionValuesSupplier {
    OutputFormatConfig getOutputFormatConfig();
    String getOutputFile(); 
}
