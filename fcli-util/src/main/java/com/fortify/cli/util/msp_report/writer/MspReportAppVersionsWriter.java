package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor;

public final class MspReportAppVersionsWriter implements IMspReportAppVersionsWriter {
    private final IRecordWriter recordWriter;
    
    public MspReportAppVersionsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/application-versions.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, MspReportSSCProcessedAppVersionDescriptor descriptor) {
        recordWriter.writeRecord(
                descriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                        .put("url", urlConfig.getUrl())));
        
    }
}
