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
package com.fortify.cli.common.rest.wait;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.progress.helper.IProgressWriterI18n;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitStatus;

public class StandardWaitHelperProgressMonitor implements IWaitHelperProgressMonitor {
    private static final OutputFormat outputFormat = OutputFormat.table;
    private final IProgressWriterI18n progressWriter;
    private final IMessageResolver messageResolver;
    private final boolean writeFinalStatus;
    
    public StandardWaitHelperProgressMonitor(IProgressWriterI18n progressWriter, IMessageResolver messageResolver, boolean writeFinalStatus) {
        this.progressWriter = progressWriter;
        this.messageResolver = messageResolver;
        this.writeFinalStatus = writeFinalStatus;
    }

    @Override
    public void updateProgress(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        if ( progressWriter!=null ) {
            try ( StringWriter sw = new StringWriter() ) {
                if ( progressWriter.isMultiLineSupported() ) {
                    writeRecordsMultiLine(recordsWithWaitStatus, sw);
                } else {
                    writeRecordsSingleLine(recordsWithWaitStatus, sw);
                }
                String output = sw.toString();
                progressWriter.writeProgress(output);
            } catch ( IOException e ) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public void finish(Map<ObjectNode, WaitStatus> recordsWithWaitStatus) {
        if ( writeFinalStatus ) {
            updateProgress(recordsWithWaitStatus);
        } else if ( progressWriter!=null ) {
            progressWriter.clearProgress();
        }
    }

    private void writeRecordsMultiLine(Map<ObjectNode, WaitStatus> recordsWithWaitStatus, StringWriter sw) {
        try ( IRecordWriter recordWriter = createRecordWriter(sw, recordsWithWaitStatus.size()==1) ) {
            for ( Map.Entry<ObjectNode, WaitStatus> entry : recordsWithWaitStatus.entrySet() ) {
                recordWriter.writeRecord(entry.getKey().put(IActionCommandResultSupplier.actionFieldName, entry.getValue().name()));
            }
        }
    }
    
    private void writeRecordsSingleLine(Map<ObjectNode, WaitStatus> recordsWithWaitStatus, StringWriter sw) {
        long completeCount = recordsWithWaitStatus.values().stream().filter(s->s==WaitStatus.WAIT_COMPLETE).count();
        // TODO Should we differentiate on failure/unknown states?
        long waitingCount = recordsWithWaitStatus.size()-completeCount;
        sw.write(String.format("Wait completed: %s, waiting: %s", completeCount, waitingCount));
    }

    private IRecordWriter createRecordWriter(StringWriter sw, boolean singular) {
        RecordWriterConfig recordWriterConfig = createRecordWriterConfig(sw, singular);
        return outputFormat.getRecordWriterFactory().createRecordWriter(recordWriterConfig);
    }
    
    private RecordWriterConfig createRecordWriterConfig(Writer writer, boolean singular) {
        return RecordWriterConfig.builder()
                .singular(singular)
                .messageResolver(messageResolver)
                .addActionColumn(true)
                .writer(writer)
                .options(null)
                .outputFormat(outputFormat)
                .build();
    }
    
}
