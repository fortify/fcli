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
