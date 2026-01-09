/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.license.ncd_report.cli.cmd;

import java.io.File;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.cli.cmd.AbstractConfigurableReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.cli.mixin.UnirestContextMixin;
import com.fortify.cli.license.ncd_report.collector.NcdReportContext;
import com.fortify.cli.license.ncd_report.config.NcdReportConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.CreateWithDetailsOutput.CMD_NAME)
public final class NcdReportCreateCommand extends AbstractConfigurableReportGenerateCommand<NcdReportConfig, NcdReportContext> {
    @Getter @Mixin private OutputHelperMixins.CreateWithDetailsOutput outputHelper;
    @Mixin private UnirestContextMixin unirestContextMixin;
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
    protected NcdReportContext createReportContext(NcdReportConfig config, IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        return new NcdReportContext(config, reportWriter, progressWriter, unirestContextMixin.getUnirestContext());
    }
}
