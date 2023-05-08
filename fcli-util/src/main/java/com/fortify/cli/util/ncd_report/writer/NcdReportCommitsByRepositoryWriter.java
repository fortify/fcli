package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportCommitDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportCommitsByRepositoryWriter implements INcdReportCommitsByRepositoryWriter {
    private final IRecordWriter recordWriter;
    
    public NcdReportCommitsByRepositoryWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/commits-by-repository.csv", false, null);
    }
    
    @Override
    public void writeRepositoryCommit(INcdReportRepositoryDescriptor repositoryDescriptor, INcdReportCommitDescriptor commitDescriptor, NcdReportProcessedAuthorDescriptor authorDescriptor) {
        recordWriter.writeRecord(authorDescriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode()
                    .put("repositoryUrl", repositoryDescriptor.getUrl())
                    .put("repositoryName", repositoryDescriptor.getFullName())
                    .put("commitId", commitDescriptor.getId())
                    .put("commitDate", commitDescriptor.getDate().toString())
                    .put("commitMessage", commitDescriptor.getMessage().split("\\R",2)[0])));
    }
}
