/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.util.entity.ncd_report.collector;

import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.report.collector.IReportResultsCollector;
import com.fortify.cli.common.report.logger.IReportLogger;
import com.fortify.cli.common.report.logger.ReportLogger;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.entity.ncd_report.cli.cmd.NcdReportGenerateCommand;
import com.fortify.cli.util.entity.ncd_report.config.NcdReportConfig;
import com.fortify.cli.util.entity.ncd_report.writer.NcdReportResultsWriters;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * This class is the primary entry point for collecting and outputting report data.
 * An instance of this class is created by the {@link NcdReportGenerateCommand}
 * and passed to the source-specific generators. Source-specific generators can use
 * this class to access the {@link IReportLogger} and 
 * {@link NcdReportRepositoryProcessor} instances.
 * 
 * @author rsenden
 *
 */
@Accessors(fluent = true)
public final class NcdReportResultsCollector implements IReportResultsCollector {
    @Getter private final NcdReportConfig reportConfig;
    @Getter private final IProgressWriterI18n progressWriter;
    private final IReportWriter reportWriter;
    private final NcdReportResultsWriters writers;
    private final NcdReportRepositoryProcessor repositoryProcessor;
    
    public NcdReportResultsCollector(NcdReportConfig reportConfig, IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        this.reportConfig = reportConfig;
        this.progressWriter = progressWriter;
        this.reportWriter = reportWriter;
        this.writers = new NcdReportResultsWriters(reportWriter, progressWriter);
        this.repositoryProcessor = new NcdReportRepositoryProcessor(reportConfig, writers, reportWriter.summary());
    }
    
    /**
     * We provide public access to {@link ReportLogger}, all
     * other writers are for internal use by this class only.
     * @return
     */
    public final IReportLogger logger() {
        return writers.logger();
    }
    
    public INcdReportRepositoryProcessor repositoryProcessor() {
        return repositoryProcessor;
    }

    @Override @SneakyThrows
    public void close() {
        repositoryProcessor.writeResults();
        logger().updateSummary(reportWriter.summary());
    }
}
