package com.fortify.cli.common.output.writer.output.standard;

import com.fortify.cli.common.output.OutputFormat;

import lombok.Data;

@Data
public final class OutputFormatConfig {
    private final OutputFormat outputFormat;
    private final String options;
}