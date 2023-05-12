package com.fortify.cli.util.msp_report.generator.ssc;

import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class MspReportSSCProcessedAppDescriptor {
    private final MspReportSSCAppDescriptor appDescriptor;
    private final MspReportProcessingStatus status;
    private final String reason;
    private final MspReportSSCAppSummaryDescriptor appSummaryDescriptor;

    public ObjectNode updateReportRecord(ObjectNode objectNode) {
        return appSummaryDescriptor.updateReportRecord(
                appDescriptor.updateReportRecord(objectNode)
                    .put("status", status.name())
                    .put("reson", reason))
                ;
    }
}
