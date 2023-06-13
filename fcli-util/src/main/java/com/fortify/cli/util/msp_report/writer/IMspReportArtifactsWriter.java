package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppVersionDescriptor;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCArtifactDescriptor;

public interface IMspReportArtifactsWriter {
    void write(IUrlConfig urlConfig, MspReportSSCAppVersionDescriptor versionDescriptor, MspReportSSCArtifactDescriptor artifactDescriptor);
}