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
package com.fortify.cli.common.report.cli.cmd;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
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
 *  <li>Calls the {@link #updateConfig(IReportSourceSupplierConfig)} method to
 *      allow sub-classes to update the configuration, for example based on CLI
 *      options</li>
 *  <li>Creates a results collector by calling 
 *      {@link #createResultsCollector(IReportSourceSupplierConfig, IReportWriter, IProgressWriterI18n)}</li>
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
    @Mixin private ProgressWriterFactoryMixin progressWriterFactory;
    
    /**
     * Generate report by instantiating progress helper, report config, 
     * results collector, and generators and running the latter.
     */
    @Override
    protected final void generateReport(IReportWriter reportWriter) {
        try ( var progressWriter = progressWriterFactory.create() ) {
            C config = getReportConfig();
            reportWriter.copyTextFile(getConfigFile().toPath(), "report-config.yaml");
            try ( var resultsCollector = createResultsCollector(config, reportWriter, progressWriter) ) {
                var sourceConfigs = config.getSourceConfigs();
                if ( sourceConfigs==null || sourceConfigs.isEmpty() ) {
                    throw new IllegalArgumentException("Configuration file doesn't define any sources");
                }
                sourceConfigs.forEach(c->runGenerator(c, resultsCollector));
            }
        }
    }

    /**
     * Run the given generator in a try-with-resources block.
     */
    private final void runGenerator(IReportSourceConfig<R> c, R resultsCollector) {
        try ( var generator = c.generator(resultsCollector) ) {
            generator.run();
        }
    }

    /**
     * Get the report configuration by deserializing the configuration
     * file returned by the {@link #getConfigFile()} method, and calling
     * the {@link #updateConfig(IReportSourceSupplierConfig)} method
     * after deserialization to allow subclasses to update the configuration.
     */
    private final C getReportConfig() {
        File configFile = getConfigFile();
        C result;
        try {
            // TODO Configure to fail on unknown properties
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            result = mapper.readValue(configFile, getConfigType());
        } catch ( Exception e ) {
            throw new IllegalStateException(String.format("Error processing configuration file %s:\n\tMessage: %s", configFile.getAbsolutePath(), e.getMessage()));
        }
        updateConfig(result);
        return result;
    }
    
    /**
     * This method can optionally be overridden by subclasses to
     * update the configuration before it is being used to generate
     * the report, for example to set CLI option values on the
     * configuration.
     */
    protected void updateConfig(C config) {}
    
    /**
     * This method must be implemented by subclasses, usually by
     * providing a CLI option field with Lombok Getter annotation.
     * We can't provide a standard option in this class, as each
     * command may have its own default option value.
     */
    protected abstract File getConfigFile();
    
    /**
     * This method must be implemented by subclasses to return a
     * {@link Class} instance that matches the generic type of this
     * class.
     */
    protected abstract Class<C> getConfigType();
    
    /**
     * This method must be implemented by subclasses to create a
     * report-specific results processor.
     */
    protected abstract R createResultsCollector(C config, IReportWriter reportWriter, IProgressWriterI18n progressWriter);
}
