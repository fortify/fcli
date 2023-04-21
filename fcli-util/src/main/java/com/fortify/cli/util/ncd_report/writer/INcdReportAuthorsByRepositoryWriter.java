package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public interface INcdReportAuthorsByRepositoryWriter {
    void writeRepositoryAuthor(INcdReportRepositoryDescriptor repositoryDescriptor, NcdReportProcessedAuthorDescriptor contributorDescriptor);
}