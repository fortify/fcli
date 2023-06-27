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
    private final IMspReportScansWriter processedScansWriter;
    private final IMspReportArtifactsWriter artifactsWriter;
    
    public MspReportResultsWriters(IReportWriter reportWriter, IProgressWriterI18n progressWriter) {
        this.progressWriter = progressWriter;
        this.logger = new ReportLogger(reportWriter, progressWriter);
        this.appsWriter = new MspReportAppsWriter(reportWriter);
        this.appVersionsWriter = new MspReportAppVersionsWriter(reportWriter);
        this.processedScansWriter = new MspReportScansWriter(reportWriter);
        this.artifactsWriter = new MspReportArtifactsWriter(reportWriter);
    }
}
