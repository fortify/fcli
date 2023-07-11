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
package com.fortify.cli.util.entity.msp_report.generator.ssc;

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
                    .put("reason", reason))
                ;
    }
}
