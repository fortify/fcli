package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.util.ncd_report.descriptor.INcdReportRepositoryDescriptor;
import com.fortify.cli.util.ncd_report.writer.NcdReportRepositoriesWriter.NcdReportRepositoryReportingStatus;

public interface INcdReportRepositoriesWriter {
    void writeRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason);
}