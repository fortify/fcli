package com.fortify.cli.util.msp_report.writer;

import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
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
    private final IProgressHelperI18n progressHelper;
    private final IReportLogger logger;
    private final IMspReportAppsWriter appsWriter;
    private final IMspReportAppVersionsWriter appVersionsWriter;
    private final IMspReportProcessedArtifactsWriter processedArtifactsWriter;
    
    public MspReportResultsWriters(IReportWriter reportWriter, IProgressHelperI18n progressHelper) {
        this.progressHelper = progressHelper;
        this.logger = new ReportLogger(reportWriter, progressHelper);
        this.appsWriter = new MspReportAppsWriter(reportWriter);
        this.appVersionsWriter = new MspReportAppVersionsWriter(reportWriter);
        this.processedArtifactsWriter = new MspReportProcessedArtifactsWriter(reportWriter);
    }
}
