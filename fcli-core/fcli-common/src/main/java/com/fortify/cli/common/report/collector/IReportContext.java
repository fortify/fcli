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
package com.fortify.cli.common.report.collector;

import com.fortify.cli.common.report.logger.IReportLogger;
import com.fortify.cli.common.rest.unirest.UnirestContext;

/**
 * Interface providing access to common context data for report generation. 
 * Implementations for specific report types usually extend this interface
 * to provide report-type-specific context data, including methods for 
 * collecting and/or writing the actual report data.
 * 
 * @author rsenden
 *
 */
public interface IReportContext extends AutoCloseable {
    /** Provide access to report logger. */
    IReportLogger logger();
    /** Provide access to Unirest context. */
    UnirestContext unirestContext();
    /** 
     *  Close current context, for example writing any (remaining) report output
     *  and closing resources. We override {@link AutoCloseable#close()} method to 
     *  not throw any exception.
     */
    void close();
}
