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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fortify.cli.common.output.cli.cmd.AbstractReportOutputCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.writer.report.IReportWriter;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperFactoryMixin;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;
import com.fortify.cli.util.ncd_report.config.NcdReportConfig;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Generate.CMD_NAME)
public final class NcdReportGenerateCommand extends AbstractReportOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Generate outputHelper;
    @Mixin private ProgressHelperFactoryMixin progressHelperFactory;
    @Option(names = {"-c","--config"}, required = true, defaultValue = "NcdReportConfig.yml")
    private File configFile;
    
    @Override
    protected String getReportTitle() {
        return "Number of Contributing Developers (NCD) Report";
    }
    
    @Override
    protected void generateReport(IReportWriter reportWriter) {
        try ( var progressHelper = progressHelperFactory.createProgressHelper() ) {
            NcdReportConfig config = getReportConfig();
            reportWriter.copyTextFile(configFile.toPath(), "report-config.yaml");
            try ( var resultsCollector = new NcdReportResultsCollector(config, reportWriter, progressHelper) ) {
                config.getSources().getSourceConfigs()
                    .forEach(c->c.generator(resultsCollector).run());
            }
        }
    }
    
    private NcdReportConfig getReportConfig() {
        try {
            // TODO Configure to fail on unknown properties
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.registerModule(new Jdk8Module());
            return mapper.readValue(configFile, NcdReportConfig.class);
        } catch ( Exception e ) {
            throw new IllegalStateException(String.format("Error processing configuration file %s:\n\tMessage: %s", configFile.getAbsolutePath(), e.getMessage()));
        }
    }
}
