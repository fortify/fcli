package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppDescriptor;

public interface IMspReportAppsWriter {
    void write(IUrlConfig urlConfig, MspReportSSCProcessedAppDescriptor descriptor);
}