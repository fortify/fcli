package com.fortify.cli.util.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class MspReportSSCProcessedAppVersionDescriptor {
    private final MspReportSSCAppVersionDescriptor appVersionDescriptor;
    private final MspReportSSCAppVersionProcessingStatus status;
    private final String reason;
    private final MspReportSSCAppVersionEntitlementSummaryDescriptor appVersionEntitlementSummaryDescriptor;

    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return appVersionEntitlementSummaryDescriptor.updateReportRecord(
                appVersionDescriptor.updateReportRecord(objectNode)
                    .put("status", status.name())
                    .put("reson", reason))
                ;
    }
    
    public static enum MspReportSSCAppVersionProcessingStatus {
        success, error
    }
}
