package com.fortify.cli.common.util.printer;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;

@ReflectiveAccess
public class PrintHelperOptions {
    @CommandLine.Option(names = {"--format"},
            description = "Output format. Possible values: json (default), yaml, table, tree.",
            defaultValue = "json", required = false, order=1)
    @Getter
    private String format;

    @CommandLine.Option(names = {"-i", "--include"},
            description = "Only used when format is \"table\". You can choose which specific columns to include for output.",
            required = false, order=2)
    @Getter
    private String include;
}
