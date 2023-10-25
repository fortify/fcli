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
package com.fortify.cli.common.report.logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.report.writer.IReportWriter;
import com.fortify.cli.common.util.Counter;

import lombok.SneakyThrows;

public final class ReportLogger implements IReportLogger {
    private final IProgressWriter progressWriter;
    private final BufferedWriter logWriter;
    private Counter errorCounter = new Counter();
    private Counter warnCounter = new Counter();

    public ReportLogger(IReportWriter reportWriter, IProgressWriter progressWriter) {
        this.logWriter = reportWriter.bufferedWriter("report.log");
        this.progressWriter = progressWriter;
    }
    
    @Override
    public void updateSummary(ObjectNode summary) {
        summary.set("logCounts", 
                JsonHelper.getObjectMapper().createObjectNode()
                    .put("error", errorCounter.getCount())
                    .put("warn", warnCounter.getCount())
                );
    }
    
    @Override
    public final void warn(String msg, Object... msgArgs) {
        write("WARN", warnCounter, msg, null, msgArgs);
    }

    @Override
    public final void warn(String msg, Exception e, Object... msgArgs) {
        write("WARN", warnCounter, msg, e, msgArgs);
    }
    
    @Override
    public final void error(String msg, Object... msgArgs) {
        write("ERROR", errorCounter, msg, null, msgArgs);
    }
    
    @Override
    public final void error(String msg, Exception e, Object... msgArgs) {
        // We want to fail completely in case of IOExceptions as this likely
        // means we cannot write the report, so we rethrow as ReportWriterIOException
        if ( e instanceof IOException ) { throw new ReportWriterIOException((IOException)e); }
        // Rethrow previously thrown ReportWriterIOException
        if ( e instanceof ReportWriterIOException ) { throw (ReportWriterIOException)e; }
        write("ERROR", errorCounter, msg, e, msgArgs);
    }
    
    @SneakyThrows
    private void write(String level, Counter counter, String msg, Exception e, Object[] msgArgs) {
        counter.increase();
        var fullMsg = msgArgs==null ? msg : String.format(msg, msgArgs);
        if ( e!=null ) {
            fullMsg = String.format("%s: %s: %s", fullMsg, e.getClass().getSimpleName(), e.getMessage());
        }
        progressWriter.writeWarning(String.format("%s: %s", level, fullMsg));
        logWriter.append(String.format("[%s] %s %s\n", LocalDateTime.now(), level, fullMsg));
        if ( e!=null ) {
            logWriter.append(String.format("%s\n", toString(e)));
        }
    }

    private String toString(Exception e) {
        try ( var sw = new StringWriter(); var pw = new PrintWriter(sw) ) {
            e.printStackTrace(pw);
            return sw.toString();
        } catch ( IOException ioe ) {
            return e.getClass().getName()+": "+e.getMessage();
        }
    }
    
    private static final class ReportWriterIOException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public ReportWriterIOException(IOException cause) {
            super("Error writing report; report contents may be incomplete", cause);
        }
    }
    
}
