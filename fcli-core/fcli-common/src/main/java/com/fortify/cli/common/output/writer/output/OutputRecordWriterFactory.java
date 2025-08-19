/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.output.writer.output;

import java.io.Writer;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle.RecordWriterStyleElement;

import lombok.Builder;

@Builder
public class OutputRecordWriterFactory {
    private final RecordWriterFactory recordWriterFactory;
    private final String recordWriterArgs;
    private final RecordWriterStyle recordWriterStyle;
    private final Supplier<Writer> writerSupplier;
    private final IMessageResolver messageResolver;
    private final boolean addActionColumn;
    private final boolean singular;
    
    public IRecordWriter createRecordWriter() {
        var config = RecordWriterConfig.builder()
            .args(resolveArgs())
            .style(resolveStyle())
            .writerSupplier(writerSupplier)
            .build();
        return recordWriterFactory.createWriter(config);
    }
    
    private RecordWriterStyle resolveStyle() {
        var newStyle = recordWriterStyle!=null ? recordWriterStyle : RecordWriterStyle.none();
        return newStyle.applyDefaultStyleElements(singular ? RecordWriterStyleElement.single : RecordWriterStyleElement.array);
    }

    private String resolveArgs() {
        return StringUtils.isNotBlank(recordWriterArgs) 
                ? recordWriterArgs 
                : getDefaultArgs();
    }

    private String getDefaultArgs() {
        var keySuffix = "output."+recordWriterFactory.toString()+".args";
        var defaultArgs = messageResolver.getMessageString(keySuffix);
        if ( addActionColumn && !StringUtils.isBlank(defaultArgs) && !defaultArgs.contains(IActionCommandResultSupplier.actionFieldName) ) {
            defaultArgs += String.format(",%s", IActionCommandResultSupplier.actionFieldName);
        }
        // TODO Only add headers for default options (i.e., apply here), 
        //      or also for user-supplied options (i.e., apply in resolveOptions())?
        //      If we change to the latter, we need to make header human-readble 
        //      formatting optional in addHeader()
        return addHeaders(defaultArgs);
    }
    
    private String addHeaders(String options) {
        if ( StringUtils.isBlank(options) ) { return null; }
        var result = new ArrayList<String>();
        for ( var o : options.split("[\\s,]+") ) {
            result.add(addHeader(o));
        }
        return String.join(",", result);
    }
    
    private String addHeader(String option) {
        if ( option.contains(":") ) { return option; } // Already contains header
        var keySuffix = "output."+recordWriterFactory.toString()+".header."+option;
        var header = resolveHeader(option, keySuffix);
        return String.format("%s:%s", option, header);
    }

    private String resolveHeader(String option, String keySuffix) {
        var header = messageResolver.getMessageString(keySuffix);
        if ( header==null ) {
            // For now, this method is only invoked for default options read from the resource
            // bundle, and resource bundles currently only contain default options for table
            // output, so we can unconditionally format as a human-readable string. However,
            // if any of the above changes, we may need to apply this formatting only for table
            // output for example.
            header = PropertyPathFormatter.humanReadable(option.replaceAll("String$", ""));
        } else if ( StringUtils.isBlank(header) ) {
            // For table outputs, we use a prefix to indicate a blank header
            header = "_."+option;
        }
        return header;
    }
}
