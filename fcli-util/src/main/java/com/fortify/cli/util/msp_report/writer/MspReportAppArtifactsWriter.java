package com.fortify.cli.util.msp_report.writer;

import java.time.LocalDateTime;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor.MspReportSSCArtifactScanType;

public final class MspReportAppArtifactsWriter implements IMspReportAppArtifactsWriter {
    private final IRecordWriter recordWriter;
    
    public MspReportAppArtifactsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, 
            MspReportSSCAppDescriptor appDescriptor, 
            MspReportSSCArtifactDescriptor artifactDescriptor, 
            MspReportSSCArtifactScanType entitlementScanType,
            boolean entitlementConsumed, 
            LocalDateTime entitlementConsumptionDate) {
        recordWriter.writeRecord(
            artifactDescriptor.updateReportRecord(
                appDescriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                            .put("url", urlConfig.getUrl())))
            .put("entitlementScanType", entitlementScanType.name())
            .put("entitlementConsumed", entitlementConsumed)
            .put("entitlementConsumptionDate", entitlementConsumptionDate==null?"N/A":entitlementConsumptionDate.toString()));        
    }
}
