package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.report.IReportWriter;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportAuthorsByRepositoryWriter implements INcdReportAuthorsByRepositoryWriter {
    private final IRecordWriter recordWriter;

    public NcdReportAuthorsByRepositoryWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/contributors-by-repository.csv", false, null);
    }
    
    @Override
    public void writeRepositoryAuthor(INcdReportRepositoryDescriptor repositoryDescriptor, NcdReportProcessedAuthorDescriptor authorDescriptor) {
        recordWriter.writeRecord(authorDescriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode()
                    .put("repositoryUrl", repositoryDescriptor.getUrl())
                    .put("repositoryName", repositoryDescriptor.getFullName())));
    }
}
