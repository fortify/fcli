/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.util.ncd_report.cli.cmd;

import java.io.File;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.cli.cmd.AbstractConfigurableReportGenerateCommand;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.ncd_report.config.NcdReportConfig;

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
