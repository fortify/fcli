/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.license.ncd_report.writer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.license.ncd_report.descriptor.NcdReportProcessedAuthorDescriptor;

public final class NcdReportAuthorsWriter implements INcdReportAuthorsWriter {
    private final IRecordWriter recordWriter;
    private final List<ObjectNode> buffer = new ArrayList<>();

    public NcdReportAuthorsWriter(IReportWriter reportWriter) {
        this.recordWriter = reportWriter.recordWriter(RecordWriterFactory.csv, "contributors.csv", false, null);
    }

    @Override
    public void writeIgnoredAuthor(NcdReportProcessedAuthorDescriptor descriptor) {
        write(descriptor, "ignored", "");
    }

    @Override
    public void writeDuplicateAuthor(NcdReportProcessedAuthorDescriptor descriptor, String representativeAuthorId,
            int contributingAuthorNumber) {
        write(descriptor, "duplicate", representativeAuthorId);
    }

    @Override
    public void writeContributor(NcdReportProcessedAuthorDescriptor descriptor, int contributingAuthorNumber) {
        write(descriptor, "contributing", "");
    }

    private void write(NcdReportProcessedAuthorDescriptor descriptor, String status, String duplicateOf) {
        var record = descriptor.updateReportRecord(JsonHelper.getObjectMapper().createObjectNode())
                .put("contributionStatus", status)
                .put("duplicateOf", duplicateOf);
        buffer.add(record);
    }

    /**
     * Close the writer by sorting all buffered records and writing them to the CSV.
     * This method should be called after all records have been buffered.
     */
    @Override
    public void close() {
        var sorted = NcdReportContributorsCsvSchema.sortByAuthorNameAndStatus(buffer);
        sorted.forEach(recordWriter::append);
    }
}
