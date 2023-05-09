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
package com.fortify.cli.common.report.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.progress.cli.mixin.ProgressHelperFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.common.report.collector.IReportResultsCollector;
import com.fortify.cli.common.report.config.IReportSourceConfig;
import com.fortify.cli.common.report.config.IReportSourceSupplierConfig;
import com.fortify.cli.common.report.writer.IReportWriter;

import picocli.CommandLine.Mixin;

/**
 * <p>Base class for commands that can generate reports based on a configuration 
 * file specifying report configuration. This is a specialization of 
 * {@link AbstractReportGenerateCommand} which:</p>
 * <ol>
 *  <li>Deserializes the configuration file returned by {@link #getConfigFile()} 
 *      into an instance of the type returned by {@link #getConfigType()}</li>
 *  <li>Creates a results collector by calling 
 *      {@link #createResultsCollector(IReportSourceSupplierConfig, IReportWriter, IProgressHelperI18n)}</li>
 *  <li>Iterates over all sources defined in the configuration<li>
 *  <li>For each source, instantiates a new generator and runs it</li>
 * </ol> 
 * 
 * @author rsenden
 *
 * @param <C> Configuration type
 * @param <R> Results collector type
 */
public abstract class AbstractConfigurableReportGenerateCommand<C extends IReportSourceSupplierConfig<R>, R extends IReportResultsCollector> extends AbstractReportGenerateCommand {
    @Mixin private ProgressHelperFactoryMixin progressHelperFactory;
    
    @Override
    protected final void generateReport(IReportWriter reportWriter) {
        try ( var progressHelper = progressHelperFactory.createProgressHelper() ) {
            C config = getReportConfig();
            reportWriter.copyTextFile(getConfigFile().toPath(), "report-config.yaml");
            try ( var resultsCollector = createResultsCollector(config, reportWriter, progressHelper) ) {
                var sourceConfigs = config.getSourceConfigs();
                if ( sourceConfigs==null || sourceConfigs.isEmpty() ) {
                    throw new IllegalArgumentException("Configuration file doesn't define any sources");
                }
                sourceConfigs.forEach(c->runGenerator(c, resultsCollector));
            }
        }
    }

    private final void runGenerator(IReportSourceConfig<R> c, R resultsCollector) {
        try ( var generator = c.generator(resultsCollector) ) {
            generator.run();
        }
    }

    private final C getReportConfig() {
        File configFile = getConfigFile();
        try {
            // TODO Configure to fail on unknown properties
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(configFile, getConfigType());
        } catch ( Exception e ) {
            throw new IllegalStateException(String.format("Error processing configuration file %s:\n\tMessage: %s", configFile.getAbsolutePath(), e.getMessage()));
        }
    }
    
    protected abstract File getConfigFile();
    protected abstract Class<C> getConfigType();
    protected abstract R createResultsCollector(C config, IReportWriter reportWriter, IProgressHelperI18n progressHelper);
}
