package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.util.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public interface INcdReportAuthorsWriter {
    void writeIgnoredAuthor(NcdReportProcessedAuthorDescriptor descriptor);
    void writeDuplicateAuthor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber);
    void writeContributor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber);
}