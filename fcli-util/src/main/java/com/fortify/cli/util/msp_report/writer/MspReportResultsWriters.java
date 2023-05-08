package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.report.writer.entry.IReportErrorEntryWriter;
import com.fortify.cli.common.report.writer.entry.ReportErrorEntryWriter;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This class holds and provides access to the individual writers.
 * 
 * @author rsenden
 *
 */
@Getter @Accessors(fluent=true)
public class MspReportResultsWriters {
    private final IProgressHelperI18n progressHelper;
    private final IReportErrorEntryWriter errorWriter;
    
    public MspReportResultsWriters(IReportWriter reportWriter, IProgressHelperI18n progressHelper) {
        this.progressHelper = progressHelper;
        this.errorWriter = new ReportErrorEntryWriter(reportWriter, progressHelper);
    }
}
