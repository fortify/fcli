package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.writer.output.OutputFormatConfigConverter.OutputFormatIterable;

import lombok.Getter;
import picocli.CommandLine.Option;

public final class OutputOptionsArgGroup implements IOutputOptionValuesSupplier {
    @Option(names = {"-o", "--output"}, order=1, converter = OutputFormatConfigConverter.class, completionCandidates = OutputFormatIterable.class, paramLabel = "format[=<options>]")
    @Getter private OutputFormatConfig outputFormatConfig;
    
    @Option(names = {"--store"}, order=1, converter = OutputStoreConfigConverter.class, paramLabel = "variableName[=<propertyNames>]")
    @Getter private OutputStoreConfig outputStoreConfig;
    
    @Option(names = {"--output-to-file"}, order=7)

    @Getter private String outputFile; 
}