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
package com.fortify.cli.util.entity.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.util.entity.ncd_report.descriptor.INcdReportRepositoryDescriptor;

public final class NcdReportRepositoriesWriter implements INcdReportRepositoriesWriter {
    private final IRecordWriter recordWriter;
    
    public NcdReportRepositoriesWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "details/repositories.csv", false, null);
    }
    
    @Override
    public void writeRepository(INcdReportRepositoryDescriptor descriptor, NcdReportRepositoryReportingStatus status, String reason) {
        recordWriter.writeRecord(JsonHelper.getObjectMapper().createObjectNode()
                    .put("repositoryUrl", descriptor.getUrl())
                    .put("repositoryName", descriptor.getFullName())
                    .put("visibility", descriptor.getVisibility())
                    .put("fork", descriptor.isFork())
                    .put("status", status.name())
                    .put("reason", reason));
    }
    
    public static enum NcdReportRepositoryReportingStatus {
        included, excluded, empty, error
    }

}
