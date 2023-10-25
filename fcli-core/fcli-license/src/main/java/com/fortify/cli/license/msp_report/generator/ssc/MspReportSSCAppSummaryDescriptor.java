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
package com.fortify.cli.license.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.Counter;

import lombok.Data;

@Data
public class MspReportSSCAppSummaryDescriptor {
    private final Counter scansProcessedCounter = new Counter();
    private final Counter scansInReportingPeriodCounter = new Counter();
    private final Counter consumedApplicationEntitlementsCounter = new Counter();
    private final Counter consumedScanEntitlementsCounter = new Counter();
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("scansProcessed", scansProcessedCounter.getCount())
                .put("scansInReportingPeriod", scansInReportingPeriodCounter.getCount())
                .put("consumedApplicationEntitlements", consumedApplicationEntitlementsCounter.getCount())
                .put("consumedScanEntitlements", consumedScanEntitlementsCounter.getCount());
    }
}
