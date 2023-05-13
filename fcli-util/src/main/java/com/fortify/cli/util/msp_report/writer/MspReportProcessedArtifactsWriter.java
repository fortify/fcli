package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.msp_report.collector.MspReportAppArtifactCollector.MspReportProcessedArtifactDescriptor;

public final class MspReportProcessedArtifactsWriter implements IMspReportProcessedArtifactsWriter {
    private final IRecordWriter processedArtifactsWriter;
    private final IRecordWriter artifactsInReportingPeriodWriter;
    private final IRecordWriter artifactsConsumingEntitlementWriter;
    
    public MspReportProcessedArtifactsWriter(IReportWriter reportWriter) {
        this.processedArtifactsWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts-processed.csv", false, null);
        this.artifactsInReportingPeriodWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts-in-reporting-period.csv", false, null);
        this.artifactsConsumingEntitlementWriter = reportWriter.recordWriter(OutputFormat.csv, "details/artifacts-consuming-entitlements.csv", false, null);
    }
    
    @Override
    public void writeProcessed(MspReportProcessedArtifactDescriptor descriptor) {
        processedArtifactsWriter.writeRecord(descriptor.getReportNode());
    }
    
    @Override
    public void writeInReportingPeriod(MspReportProcessedArtifactDescriptor descriptor) {
        artifactsInReportingPeriodWriter.writeRecord(descriptor.getReportNode());
    }
    
    @Override
    public void writeEntitlementConsuming(MspReportProcessedArtifactDescriptor descriptor) {
        artifactsConsumingEntitlementWriter.writeRecord(descriptor.getReportNode());
    }
}
