/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.output.cli.mixin;

import java.io.File;

import com.fortify.cli.common.mcp.MCPExclude;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.writer.output.standard.IOutputOptions;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfig;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfigConverter;
import com.fortify.cli.common.output.writer.output.standard.OutputFormatConfigConverter.OutputFormatIterable;
import com.fortify.cli.common.output.writer.output.standard.VariableStoreConfig;
import com.fortify.cli.common.output.writer.output.standard.VariableStoreConfigConverter;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle.RecordWriterStyleElement;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * This class (used as an {@link ArgGroup}) defines common fcli output options
 * that are available on every fcli command that extends from
 * {@link AbstractOutputCommand}.
 *
 * We {@link MCPExclude} most output options as we hardcode output format to
 * JSON for MCP responses, and other options are (likely) not relevant in
 * LLM/MCP environments.
 */
public final class OutputOptionsArgGroup implements IOutputOptions {
    @Option(names = {"-o", "--output"}, order = 1, converter = OutputFormatConfigConverter.class, completionCandidates = OutputFormatIterable.class, paramLabel = "<type+args>")
    @MCPExclude
    @Getter
    private OutputFormatConfig outputFormatConfig;

    @Option(names = {"--style"}, split = ",", order = 2, paramLabel = "<style>,...", hideParamSyntax = true)
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @MCPExclude
    @Getter
    private RecordWriterStyleElement[] outputStyleElements;

    @Option(names = {"--store"}, order = 3, converter = VariableStoreConfigConverter.class, paramLabel = "<var>[:<prop>]")
    @Getter
    private VariableStoreConfig variableStoreConfig;

    @Option(names = {"--to-file"}, order = 4)
    @MCPExclude
    @Getter
    private File outputFile;
}
