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
package com.fortify.cli.util.entity.ncd_report.cli.cmd;

import java.io.File;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.cli.cmd.AbstractConfigurableReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.entity.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.entity.ncd_report.config.NcdReportConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Generate.CMD_NAME)
public final class NcdReportGenerateCommand extends AbstractConfigurableReportGenerateCommand<NcdReportConfig, NcdReportResultsCollector> {
    @Getter @Mixin private OutputHelperMixins.Generate outputHelper;
    @Option(names = {"-c","--config"}, required = true, defaultValue = "NcdReportConfig.yml")
    @Getter private File configFile;
    
    @Override
    protected String getReportTitle() {
        return "Number of Contributing Developers (NCD) Report";
    }
    
    @Override
    protected Class<NcdReportConfig> getConfigType() {
        return NcdReportConfig.class;
    }
    
    @Override
    protected NcdReportResultsCollector createResultsCollector(NcdReportConfig config, IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        return new NcdReportResultsCollector(config, reportWriter, progressWriter);
    }
}
