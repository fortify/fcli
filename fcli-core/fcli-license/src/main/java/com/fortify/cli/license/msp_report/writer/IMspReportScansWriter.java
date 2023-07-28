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
package com.fortify.cli.license.msp_report.writer;

import com.fortify.cli.license.msp_report.collector.MspReportAppScanCollector.MspReportProcessedScanDescriptor;

public interface IMspReportScansWriter {
    void writeProcessed(MspReportProcessedScanDescriptor descriptor);
    void writeInReportingPeriod(MspReportProcessedScanDescriptor descriptor);
    void writeEntitlementConsuming(MspReportProcessedScanDescriptor descriptor);
}