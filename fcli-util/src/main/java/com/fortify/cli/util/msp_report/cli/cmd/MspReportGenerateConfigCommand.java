/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.msp_report.cli.cmd;

import com.fortify.cli.common.output.cli.cmd.AbstractGenerateConfigCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.GenerateConfig.CMD_NAME)
public class MspReportGenerateConfigCommand extends AbstractGenerateConfigCommand {
    @Getter @Mixin private OutputHelperMixins.GenerateConfig outputHelper;
    @Getter private final String resourceFileName = "com/fortify/cli/util/msp_report/MspReportConfig.yml";

    @Option(names = {"-c", "--config"}, defaultValue = "MspReportConfig.yml") 
    @Getter private String outputFileName;
}