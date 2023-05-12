package com.fortify.cli.util.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.util.Counter;

import lombok.Data;

@Data
public class MspReportSSCAppSummaryDescriptor {
    private final Counter artifactsProcessedCounter = new Counter();
    private final Counter artifactsInReportingPeriodCounter = new Counter();
    private final Counter consumedApplicationEntitlementsCounter = new Counter();
    private final Counter consumedScanEntitlementsCounter = new Counter();
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("artifactsProcessed", artifactsProcessedCounter.getCount())
                .put("artifactsInReportingPeriod", artifactsInReportingPeriodCounter.getCount())
                .put("consumedApplicationEntitlements", consumedApplicationEntitlementsCounter.getCount())
                .put("consumedScanEntitlements", consumedScanEntitlementsCounter.getCount());
    }
}
