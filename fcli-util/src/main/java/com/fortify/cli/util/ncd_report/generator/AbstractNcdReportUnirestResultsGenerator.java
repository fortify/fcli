package com.fortify.cli.util.ncd_report.generator;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.util.ncd_report.collector.NcdReportResultsCollector;

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
public abstract class AbstractNcdReportUnirestResultsGenerator<T extends IUrlConfig> implements Runnable {
    @Getter private final T sourceConfig;
    @Getter private final NcdReportResultsCollector resultsCollector;
    
    /**
     * Primary method for running the generation process. This method
     * initializes a {@link UnirestInstance} based on the given source
     * configuration, then calls the abstract {@link #run(UnirestInstance)} 
     * method for which the implementation is provided by a source-specific
     * subclass.
     */
    @Override
    public final void run() {
        try ( var unirest = createUnirestInstance() ) {
            run(unirest);
        } catch ( Exception e ) {
            handleSourceError(e);
        }
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
        resultsCollector().errorWriter().addReportError(String.format("Error processing %s source: %s", getType(), sourceConfig().getUrl()), e);
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
     * Method to be implemented by subclasses to run the generation
     * process using the given {@link UnirestInstance}.
     */
    protected abstract void run(UnirestInstance unirest);
    
    /**
     * Method to be implemented by subclasses to return the source type,
     * i.e. 'github', 'gitlab', ... This is used for proxy configuration
     * and error reporting.
     */
    protected abstract String getType();
    
    
}
