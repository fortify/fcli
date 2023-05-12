package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppDescriptor;

public final class MspReportAppsWriter implements IMspReportAppsWriter {
    private final IRecordWriter recordWriter;
    
    public MspReportAppsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/applications.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, MspReportSSCProcessedAppDescriptor descriptor) {
        recordWriter.writeRecord(
                descriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                        .put("url", urlConfig.getUrl())));
        
    }
}
