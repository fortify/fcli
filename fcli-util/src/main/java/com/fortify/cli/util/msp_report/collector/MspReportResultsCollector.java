/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.msp_report.collector;

import java.time.format.DateTimeFormatter;

import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
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
    @Getter private final IProgressWriterI18n progressWriter;
    private final IReportWriter reportWriter;
    private final MspReportResultsWriters writers;
    @Getter private final MspReportAppCollector appCollector;
    @Getter private final MspReportAppVersionCollector appVersionCollector;
    @Getter private final MspReportArtifactCollector artifactCollector;
    
    public MspReportResultsCollector(MspReportConfig reportConfig, IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        this.reportConfig = reportConfig;
        this.progressWriter = progressWriter;
        this.reportWriter = reportWriter;
        this.writers = new MspReportResultsWriters(reportWriter, progressWriter);
        this.appCollector = new MspReportAppCollector(this.writers, reportWriter.summary());
        this.appVersionCollector = new MspReportAppVersionCollector(this.writers, reportWriter.summary());
        this.artifactCollector = new MspReportArtifactCollector(this.writers, reportWriter.summary());
    }
    
    public MspReportAppScanCollector scanCollector(IUrlConfig urlConfig, MspReportSSCAppDescriptor appDescriptor) {
        return new MspReportAppScanCollector(reportConfig, writers, urlConfig, appDescriptor);
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
        artifactCollector.writeResults();
        logger().updateSummary(reportWriter.summary());
    }
}
