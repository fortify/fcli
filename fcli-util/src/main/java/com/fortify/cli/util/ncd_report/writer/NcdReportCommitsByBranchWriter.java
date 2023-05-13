package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportCommitsByBranchWriter implements INcdReportCommitsByBranchWriter {
    private final IRecordWriter recordWriter;

    public NcdReportCommitsByBranchWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/commits-by-branch.csv", false, null);
    }
    
    @Override
    public void writeBranchCommit(NcdReportBranchCommitDescriptor descriptor, NcdReportProcessedAuthorDescriptor authorDescriptor) {
        var repositoryDescriptor = descriptor.getRepositoryDescriptor();
        var branchDescriptor = descriptor.getBranchDescriptor();
        var commitDescriptor = descriptor.getCommitDescriptor();
        recordWriter.writeRecord(authorDescriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode()
                    .put("repositoryUrl", repositoryDescriptor.getUrl())
                    .put("repositoryName", repositoryDescriptor.getFullName())
                    .put("branchName", branchDescriptor.getName())
                    .put("commitId", commitDescriptor.getId())
                    .put("commitDate", commitDescriptor.getDate().toString())
                    .put("commitMessage", commitDescriptor.getMessage().split("\\R",2)[0])));
    }
}