package com.fortify.cli.common.output.writer.output;

import com.fortify.cli.common.output.writer.OutputFormat;

import lombok.Data;

@Data
public final class OutputFormatConfig {
    private final OutputFormat outputFormat;
    private final String options;
}