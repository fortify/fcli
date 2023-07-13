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
package com.fortify.cli.common.report.logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IReportLogger {
    void warn(String msg, Object... args);
    void warn(String msg, Exception e, Object... args);
    void error(String msg, Object... args);
    void error(String msg, Exception e, Object... args);
    void updateSummary(ObjectNode summary);
}