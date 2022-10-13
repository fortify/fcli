package com.fortify.cli.common.output.writer.output.standard;

import lombok.Data;

@Data
public final class VariableStoreConfig {
    private final String variableName;
    private final String options;
}