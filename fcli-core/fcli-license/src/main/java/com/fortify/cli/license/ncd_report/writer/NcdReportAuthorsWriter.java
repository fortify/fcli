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
package com.fortify.cli.license.ncd_report.writer;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportAuthorsWriter implements INcdReportAuthorsWriter {
    private final IRecordWriter recordWriter;

    public NcdReportAuthorsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "contributors.csv", false, null);
    }
    
    @Override
    public void writeIgnoredAuthor(NcdReportProcessedAuthorDescriptor descriptor) {
        write(descriptor, "ignored", -1);
    }
    
    @Override
    public void writeDuplicateAuthor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber) {
        write(descriptor, "duplicate", contributingAuthorNumber);
    }
    
    @Override
    public void writeContributor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber) {
        write(descriptor, "contributing", contributingAuthorNumber);
    }
    
    public void write(NcdReportProcessedAuthorDescriptor descriptor, String status, int contributingAuthorNumber) {
        recordWriter.writeRecord(descriptor.updateReportRecord(
                JsonHelper.getObjectMapper().createObjectNode())
                .put("contributionStatus", status)
                .put("contributingAuthorNumber", contributingAuthorNumber));
    }
}
