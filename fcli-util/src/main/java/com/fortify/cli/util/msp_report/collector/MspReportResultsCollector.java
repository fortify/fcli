package com.fortify.cli.util.msp_report.collector;

import java.time.format.DateTimeFormatter;

import com.fortify.cli.common.progress.helper.IProgressHelperI18n;
import com.fortify.cli.common.report.collector.IReportResultsCollector;
import com.fortify.cli.common.report.logger.IReportLogger;
import com.fortify.cli.common.report.logger.ReportLogger;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.util.msp_report.cli.cmd.MspReportGenerateCommand;
import com.fortify.cli.util.msp_report.config.MspReportConfig;
import com.fortify.cli.util.msp_report.generator.ssc.MspReportSSCAppDescriptor;
import com.fortify.cli.util.msp_report.writer.MspReportResultsWriters;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * This class is the primary entry point for collecting and outputting report data.
 * An instance of this class is created by the {@link MspReportGenerateCommand}
 * and passed to the source-specific generators. Source-specific generators can use
 * this class to access the {@link IReportLogger} and various result collectors.
 * 
 * @author rsenden
 *
 */
@Accessors(fluent = true)
public final class MspReportResultsCollector implements IReportResultsCollector {
    @Getter private final MspReportConfig reportConfig;
    @Getter private final IProgressHelperI18n progressHelper;
    private final IReportWriter reportWriter;
    private final MspReportResultsWriters writers;
    @Getter private final MspReportAppCollector appCollector;
    @Getter private final MspReportAppVersionCollector appVersionCollector;
    
    public MspReportResultsCollector(MspReportConfig reportConfig, IReportWriter reportWriter, IProgressHelperI18n progressHelper) {
        this.reportConfig = reportConfig;
        this.progressHelper = progressHelper;
        this.reportWriter = reportWriter;
        this.writers = new MspReportResultsWriters(reportWriter, progressHelper);
        this.appCollector = new MspReportAppCollector(this.writers, reportWriter.summary());
        this.appVersionCollector = new MspReportAppVersionCollector(this.writers, reportWriter.summary());
    }
    
    public MspReportAppArtifactCollector artifactCollector(IUrlConfig urlConfig, MspReportSSCAppDescriptor appDescriptor) {
        return new MspReportAppArtifactCollector(reportConfig, writers, urlConfig, appDescriptor);
    }
    
    /**
     * We provide public access to {@link ReportLogger}, all
     * other writers are for internal use by this class only.
     * @return
     */
    public final IReportLogger logger() {
        return writers.logger();
    }

    @Override @SneakyThrows
    public void close() {
        reportWriter.summary().put("mspName", reportConfig.getMspName());
        reportWriter.summary().put("contractStartDate", reportConfig.getContractStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        reportWriter.summary().put("reportingStartDate", reportConfig.getReportingStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        reportWriter.summary().put("reportingEndDate", reportConfig.getReportingEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        appCollector.writeResults();
        appVersionCollector.writeResults();
        logger().updateSummary(reportWriter.summary());
    }
}
