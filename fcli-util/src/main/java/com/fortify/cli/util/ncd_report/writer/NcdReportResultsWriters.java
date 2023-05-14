package com.fortify.cli.util.ncd_report.writer;

import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.logger.IReportLogger;
import com.fortify.cli.common.report.logger.ReportLogger;
import com.fortify.cli.common.report.writer.IReportWriter;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * This class holds and provides access to the individual writers.
 * 
 * @author rsenden
 *
 */
@Getter @Accessors(fluent=true)
public class NcdReportResultsWriters {
    private final IProgressWriterI18n progressWriter;
    private final IReportLogger logger;
    private final INcdReportRepositoriesWriter repositoryWriter;
    private final INcdReportCommitsByBranchWriter commitsByBranchWriter;
    private final INcdReportCommitsByRepositoryWriter commitsByRepositoryWriter;
    private final INcdReportAuthorsByRepositoryWriter authorsByRepositoryWriter;
    private final INcdReportAuthorsWriter authorsWriter;
    
    public NcdReportResultsWriters(IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        this.progressWriter = progressWriter;
        this.logger = new ReportLogger(reportWriter, progressWriter);
        this.repositoryWriter = new NcdReportRepositoriesWriter(reportWriter);
        this.commitsByBranchWriter = new NcdReportCommitsByBranchWriter(reportWriter);
        this.commitsByRepositoryWriter = new NcdReportCommitsByRepositoryWriter(reportWriter);
        this.authorsByRepositoryWriter = new NcdReportAuthorsByRepositoryWriter(reportWriter);
        this.authorsWriter = new NcdReportAuthorsWriter(reportWriter);
    }
}
