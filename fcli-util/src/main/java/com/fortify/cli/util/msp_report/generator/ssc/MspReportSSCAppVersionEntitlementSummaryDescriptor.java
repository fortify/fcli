package com.fortify.cli.util.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @RequiredArgsConstructor
public class MspReportSSCAppVersionEntitlementSummaryDescriptor {
    private final MspReportLicenseType mspLicenseType;
    private int numberOfScansInReportingPeriod;
    
    public void increaseNumberOfScansInReportingPeriod() {
        numberOfScansInReportingPeriod++;
    }
    
    public int getApplicationEntitlementsConsumed() {
        return mspLicenseType==null || mspLicenseType==MspReportLicenseType.Application 
                ? 1
                : 0;
    }
    
    public int getScanEntitlementsConsumed() {
        return mspLicenseType==MspReportLicenseType.Scan 
                ? numberOfScansInReportingPeriod
                : 0;
    }
    
    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return objectNode
                .put("numberOfScansInReportingPeriod", numberOfScansInReportingPeriod)
                .put("applicationEntitlementsConsumed", getApplicationEntitlementsConsumed())
                .put("scanEntitlementsConsumed", getScanEntitlementsConsumed());
    }
}
