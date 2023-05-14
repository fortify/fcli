package com.fortify.cli.util.msp_report.writer;

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
public class MspReportResultsWriters {
    private final IProgressWriterI18n progressWriter;
    private final IReportLogger logger;
    private final IMspReportAppsWriter appsWriter;
    private final IMspReportAppVersionsWriter appVersionsWriter;
    private final IMspReportProcessedArtifactsWriter processedArtifactsWriter;
    
    public MspReportResultsWriters(IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        this.progressWriter = progressWriter;
        this.logger = new ReportLogger(reportWriter, progressWriter);
        this.appsWriter = new MspReportAppsWriter(reportWriter);
        this.appVersionsWriter = new MspReportAppVersionsWriter(reportWriter);
        this.processedArtifactsWriter = new MspReportProcessedArtifactsWriter(reportWriter);
    }
}
