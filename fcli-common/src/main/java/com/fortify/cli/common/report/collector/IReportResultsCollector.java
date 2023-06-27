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
package com.fortify.cli.common.report.collector;

import com.fortify.cli.common.report.logger.IReportLogger;

public interface IReportResultsCollector extends AutoCloseable {
    IReportLogger logger();
    /**
     * Override default close method to not throw any exception.
     */
    void close();
}
