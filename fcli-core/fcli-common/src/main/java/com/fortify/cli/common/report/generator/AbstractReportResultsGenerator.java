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
package com.fortify.cli.common.report.generator;

import com.fortify.cli.common.report.collector.IReportContext;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Base class for source-specific unirest-based generator implementations, 
 * providing functionality for storing and accessing the report configuration, 
 * and for creating unirest instances based on connection settings defined in 
 * the configuration file.
 *  
 * @author rsenden
 */
@RequiredArgsConstructor @Accessors(fluent=true)
public abstract class AbstractReportResultsGenerator<C extends IUrlConfig, R extends IReportContext> implements IReportResultsGenerator {
    @Getter private final C sourceConfig;
    @Getter private final R reportContext;
    
    /**
     * Primary method for running the generation process. This method
     * initializes a {@link UnirestInstance} based on the given source
     * configuration, then calls the abstract {@link #run(UnirestInstance)} 
     * method for which the implementation is provided by a source-specific
     * subclass.
     */
    @Override
    public final void run() {
        try {
            generateResults();
        } catch ( Exception e ) {
            handleSourceError(e);
        }
    }
    
    @Override // TODO Do we need results generators to be closeable?
    public void close() {}
    
    /**
     * Handle the given {@link Exception} that may potentially be thrown by 
     * the {@link #run(UnirestInstance)} method, adding the error to the
     * report output. 
     */
    private final void handleSourceError(Exception e) {
        reportContext().logger().error(String.format("Error processing %s source: %s", getType(), sourceConfig().getUrl()), e);
    }
    
    /**
     * Method to be implemented by subclasses to generate results. Implementations
     * can call {@link #unirest()} to access the {@link UnirestInstance}.
     */
    protected abstract void generateResults();
    
    /**
     * Method to be implemented by subclasses to return the source type, for example
     * 'ssc', 'github', 'gitlab', ... This is used for error reporting.
     */
    protected abstract String getType();
    
    
}
