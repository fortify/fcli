package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.util.msp_report.collector.MspReportAppArtifactCollector.MspReportProcessedArtifactDescriptor;

public interface IMspReportProcessedArtifactsWriter {
    void writeProcessed(MspReportProcessedArtifactDescriptor descriptor);
    void writeInReportingPeriod(MspReportProcessedArtifactDescriptor descriptor);
    void writeEntitlementConsuming(MspReportProcessedArtifactDescriptor descriptor);
}