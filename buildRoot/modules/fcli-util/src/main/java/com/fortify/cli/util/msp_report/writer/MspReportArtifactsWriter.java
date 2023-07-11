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
package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;

public final class MspReportArtifactsWriter implements IMspReportArtifactsWriter {
    private final IRecordWriter artifactsRecordWriter;
    private final IRecordWriter artifactsWithoutScansRecordWriter;
    
    public MspReportArtifactsWriter(IReportWriter reportWriter) {
        this.artifactsRecordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts.csv", false, null);
        this.artifactsWithoutScansRecordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts-without-scans.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, MspReportSSCAppVersionDescriptor versionDescriptor, MspReportSSCArtifactDescriptor artifactDescriptor) {
        var record = 
            artifactDescriptor.updateReportRecord(
                    versionDescriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                        .put("url", urlConfig.getUrl())));
        artifactsRecordWriter.writeRecord(record);
        if ( !artifactDescriptor.hasScans() ) {
            artifactsWithoutScansRecordWriter.writeRecord(record);
        }
    }
}
