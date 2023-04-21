package com.fortify.cli.util.ncd_report.collector;

import com.fortify.cli.common.output.writer.report.IReportWriter;
import com.fortify.cli.common.output.writer.report.entry.IReportErrorEntryWriter;
import com.fortify.cli.common.output.writer.report.entry.ReportErrorEntryWriter;
import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.util.ncd_report.cli.cmd.NcdReportGenerateCommand;
import com.fortify.cli.util.ncd_report.config.NcdReportConfig;
import com.fortify.cli.util.ncd_report.writer.NcdReportResultsWriters;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * This class is the primary entry point for collecting and outputting report data.
 * An instance of this class is created by the {@link NcdReportGenerateCommand}
 * and passed to the source-specific generators. Source-specific generators can use
 * this class to access the {@link IReportErrorEntryWriter} and 
 * {@link NcdReportRepositoryProcessor} instances.
 * 
 * @author rsenden
 *
 */
@Accessors(fluent = true)
public final class NcdReportResultsCollector implements AutoCloseable {
    @Getter private final NcdReportConfig reportConfig;
    @Getter private final IProgressHelperI18n progressHelper;
    private final IReportWriter reportWriter;
    private final NcdReportResultsWriters writers;
    private final NcdReportRepositoryProcessor repositoryProcessor;
    
    public NcdReportResultsCollector(NcdReportConfig reportConfig, IReportWriter reportWriter, IProgressHelperI18n progressHelper) {
        this.reportConfig = reportConfig;
        this.progressHelper = progressHelper;
        this.reportWriter = reportWriter;
        this.writers = new NcdReportResultsWriters(reportWriter, progressHelper);
        this.repositoryProcessor = new NcdReportRepositoryProcessor(reportConfig, writers, reportWriter.summary());
    }
    
    /**
     * We provide public access to {@link ReportErrorEntryWriter}, all
     * other writers are for internal use by this class only.
     * @return
     */
    public final IReportErrorEntryWriter errorWriter() {
        return writers.errorWriter();
    }
    
    public INcdReportRepositoryProcessor repositoryProcessor() {
        return repositoryProcessor;
    }

    @Override @SneakyThrows
    public void close() {
        reportWriter.summary().put("errorCount", errorWriter().getErrorCount());
        repositoryProcessor.writeResults();
    }
}
