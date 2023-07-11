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
package com.fortify.cli.util.msp_report.cli.cmd;

import java.io.File;
import java.time.LocalDate;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.cli.cmd.AbstractConfigurableReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.msp_report.collector.MspReportResultsCollector;
import com.fortify.cli.util.msp_report.config.MspReportConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Generate.CMD_NAME)
public final class MspReportGenerateCommand extends AbstractConfigurableReportGenerateCommand<MspReportConfig, MspReportResultsCollector> {
    @Getter @Mixin private OutputHelperMixins.Generate outputHelper;
    @Option(names = {"-c","--config"}, required = true, defaultValue = "MspReportConfig.yml")
    @Getter private File configFile;
    @Option(names = {"-s","--start-date"}, required = true)
    private LocalDate reportingStartDate;
    @Option(names = {"-e","--end-date"}, required = true)
    private LocalDate reportingEndDate;
    
    @Override
    protected String getReportTitle() {
        return "Managed Service Provider (MSP) Report";
    }
    
    @Override
    protected void updateConfig(MspReportConfig config) {
        config.setReportingStartDate(reportingStartDate);
        config.setReportingEndDate(reportingEndDate);
        config.validate();
    }
    
    @Override
    protected Class<MspReportConfig> getConfigType() {
        return MspReportConfig.class;
    }
    
    @Override
    protected MspReportResultsCollector createResultsCollector(MspReportConfig config, IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        return new MspReportResultsCollector(config, reportWriter, progressWriter);
    }
}
