package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.cli.mixin.OutputMixin.OutputFormatConfig;
import com.fortify.cli.common.output.writer.output.OutputFormatConfigConverter.OutputFormatIterable;

import lombok.Getter;
import picocli.CommandLine;

public final class OutputOptionsArgGroup implements IOutputOptionValuesSupplier {
    @CommandLine.Option(names = {"-o", "--output"}, order=1, converter = OutputFormatConfigConverter.class, completionCandidates = OutputFormatIterable.class, paramLabel = "format[=<options>]")
    @Getter private OutputFormatConfig outputFormatConfig;
    
    @CommandLine.Option(names = {"--output-to-file"}, order=7)
    @Getter private String outputFile; 
}