package com.fortify.cli.util.msp_report.writer;

import java.time.LocalDateTime;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor.MspReportSSCArtifactScanType;

public interface IMspReportAppArtifactsWriter {
    void write(IUrlConfig urlConfig, 
            MspReportSSCAppDescriptor appDescriptor, 
            MspReportSSCArtifactDescriptor artifactDescriptor, 
            MspReportSSCArtifactScanType entitlementScanType,
            boolean entitlementConsumed, 
            LocalDateTime entitlementConsumptionDate);
}