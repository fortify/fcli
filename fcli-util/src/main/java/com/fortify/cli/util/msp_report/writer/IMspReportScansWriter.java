package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.util.msp_report.collector.MspReportAppScanCollector.MspReportProcessedScanDescriptor;

public interface IMspReportScansWriter {
    void writeProcessed(MspReportProcessedScanDescriptor descriptor);
    void writeInReportingPeriod(MspReportProcessedScanDescriptor descriptor);
    void writeEntitlementConsuming(MspReportProcessedScanDescriptor descriptor);
}