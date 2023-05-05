package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.report.IReportWriter;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportAuthorsWriter implements INcdReportAuthorsWriter {
    private final IRecordWriter recordWriter;

    public NcdReportAuthorsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "contributors.csv", false, null);
    }
    
    @Override
    public void writeIgnoredAuthor(NcdReportProcessedAuthorDescriptor descriptor) {
        write(descriptor, "ignored", -1);
    }
    
    @Override
    public void writeDuplicateAuthor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber) {
        write(descriptor, "duplicate", contributingAuthorNumber);
    }
    
    @Override
    public void writeContributor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber) {
        write(descriptor, "contributing", contributingAuthorNumber);
    }
    
    public void write(NcdReportProcessedAuthorDescriptor descriptor, String status, int contributingAuthorNumber) {
        recordWriter.writeRecord(descriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode())
                .put("contributionStatus", status)
                .put("contributingAuthorNumber", contributingAuthorNumber));
    }
}
