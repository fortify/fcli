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
package com.fortify.cli.common.report.generator;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.report.collector.IReportResultsCollector;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
public abstract class AbstractReportUnirestResultsGenerator<T extends IUrlConfig, R extends IReportResultsCollector> implements IReportResultsGenerator {
    @Getter private final T sourceConfig;
    @Getter private final R resultsCollector;
    private UnirestInstance unirest;
    
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
    
    @Override @SneakyThrows
    public final void close() {
        if ( unirest!=null ) {
            unirest.close();
        }
    }
    
    protected final UnirestInstance unirest() {
        if ( unirest==null ) {
            unirest = createUnirestInstance();
        }
        return unirest;
    }
    
    /**
     * Create a new {@link UnirestInstance} and configure it using the
     * {@link #_configure(UnirestInstance)} method.
     * @return
     */
    private final UnirestInstance createUnirestInstance() {
        return _configure(GenericUnirestFactory.createUnirestInstance());
    }
    
    /**
     * Handle the given {@link Exception} that may potentially be thrown by 
     * the {@link #run(UnirestInstance)} method, adding the error to the
     * report output. 
     */
    private final void handleSourceError(Exception e) {
        resultsCollector().logger().error(String.format("Error processing %s source: %s", getType(), sourceConfig().getUrl()), e);
    }
    
    /**
     * Configure the given {@link UnirestInstance} based on the provided 
     * {@link IUrlConfig} configuration. Sub-classes must implement the 
     * {@link #configure(UnirestInstance)} method to perform additional 
     * configuration, like adding source-specific authorization header 
     * configuration.
     * @return Configured {@link UnirestInstance}
     */
    private final UnirestInstance _configure(UnirestInstance unirest) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sourceConfig());
        ProxyHelper.configureProxy(unirest, getType(), sourceConfig().getUrl());
        configure(unirest);
        return unirest;
    }
    
    /**
     * This base class performs basic configuration of a {@link UnirestInstance}
     * based on the given {@link IUrlConfig}. Subclasses must implement this
     * method to perform any additional source-specific configuration. Most 
     * commonly, implementions of this method would configure source-specific
     * authorization headers, and optionally configure interceptors for 
     * handling rate limiting for example.
     */
    protected abstract void configure(UnirestInstance unirest);
    
    /**
     * Method to be implemented by subclasses to generate results. Implementations
     * can call {@link #unirest()} to access the {@link UnirestInstance}.
     */
    protected abstract void generateResults();
    
    /**
     * Method to be implemented by subclasses to return the source type, for example
     * 'ssc', 'github', 'gitlab', ... This is used for proxy configuration and error 
     * reporting.
     */
    protected abstract String getType();
    
    
}
