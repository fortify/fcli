package com.fortify.cli.common.output.cli.mixin.impl;

import com.fortify.cli.common.output.writer.output.standard.IOutputOptions;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfig;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfigConverter;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfigConverter.OutputFormatIterable;
import com.fortify.cli.common.output.writer.output.standard.VariableStoreConfig;
import com.fortify.cli.common.output.writer.output.standard.VariableStoreConfigConverter;

import lombok.Getter;
import picocli.CommandLine.Option;

public final class OutputOptionsArgGroup implements IOutputOptions {
    @Option(names = {"-o", "--output"}, order=1, converter = OutputFormatConfigConverter.class, completionCandidates = OutputFormatIterable.class, paramLabel = "format[=<options>]")
    @Getter private OutputFormatConfig outputFormatConfig;
    
    @Option(names = {"--store"}, order=1, converter = VariableStoreConfigConverter.class, paramLabel = "variableName[=<propertyNames>]")
    @Getter private VariableStoreConfig variableStoreConfig;
    
    @Option(names = {"--output-to-file"}, order=7)

    @Getter private String outputFile; 
}