package com.fortify.cli.common.output.writer.report.entry;

import java.io.IOException;

import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.report.IReportWriter;
import com.fortify.cli.common.progress.helper.IProgressHelper;

import lombok.Getter;

public final class ReportErrorEntryWriter implements IReportErrorEntryWriter {
    private IProgressHelper progressHelper;
    private IRecordWriter recordWriter;
    @Getter private int errorCount = 0;

    public ReportErrorEntryWriter(IReportWriter reportWriter, IProgressHelper progressHelper) {
        this.recordWriter = reportWriter.recordWriter(OutputFormat.csv, "report-errors.csv", false, null);
        this.progressHelper = progressHelper;
    }
    
    @Override
    public final void addReportError(String operation, String message) {
        errorCount++;
        progressHelper.writeWarning("WARN: %s: %s", operation, message);
        recordWriter.writeRecord(JsonHelper.getObjectMapper().createObjectNode()
                .put("operation", operation)
                .put("message", message));
    }
    
    @Override
    public final void addReportError(String operation, Exception e) {
        if ( e instanceof IOException ) { throw new ReportWriterIOException((IOException)e); }
        if ( e instanceof ReportWriterIOException ) { throw (ReportWriterIOException)e; }
        addReportError(operation, e.getClass().getName()+": "+e.getMessage());
    }
    
    public static final class ReportWriterIOException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public ReportWriterIOException(IOException cause) {
            super("Error writing report; report contents may be incomplete", cause);
        }
    }
    
}
