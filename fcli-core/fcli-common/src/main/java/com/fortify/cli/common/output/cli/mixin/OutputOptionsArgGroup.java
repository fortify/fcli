/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.cli.mixin;

import java.io.File;

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
    
    @Option(names = {"--to-file"}, order=7)
    @Getter private File outputFile; 
}