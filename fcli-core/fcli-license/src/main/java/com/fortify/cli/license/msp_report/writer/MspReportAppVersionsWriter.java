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
package com.fortify.cli.license.msp_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.license.msp_report.generator.ssc.MspReportSSCProcessedAppVersionDescriptor;

public final class MspReportAppVersionsWriter implements IMspReportAppVersionsWriter {
    private final IRecordWriter recordWriter;
    
    public MspReportAppVersionsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/application-versions.csv", false, null);
    }
    
    @Override
    public void write(IUrlConfig urlConfig, MspReportSSCProcessedAppVersionDescriptor descriptor) {
        recordWriter.writeRecord(
                descriptor.updateReportRecord(
                        JsonHelper.getObjectMapper().createObjectNode()
                        .put("url", urlConfig.getUrl())));
        
    }
}
