package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.util.ncd_report.descriptor.NcdReportBranchCommitDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public interface INcdReportCommitsByBranchWriter {
    void writeBranchCommit(NcdReportBranchCommitDescriptor descriptor, NcdReportProcessedAuthorDescriptor processedAuthorDescriptor);
}