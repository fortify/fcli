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

import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.PropertyPathFormatter;
import com.fortify.cli.common.output.writer.IMessageResolver;
import com.fortify.cli.common.output.writer.record.IRecordWriter;
import com.fortify.cli.common.output.writer.record.RecordWriterConfig;
import com.fortify.cli.common.output.writer.record.RecordWriterFactory;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle;
import com.fortify.cli.common.output.writer.record.RecordWriterStyle.RecordWriterStyleElement;
import com.fortify.cli.common.util.StringUtils;

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
        // TODO Always add action column, or only on default options?
        return addActionColumn(StringUtils.isNotBlank(recordWriterArgs) 
                ? recordWriterArgs 
                : getDefaultArgs());
    }
    
    private String addActionColumn(String options) {
        if ( addActionColumn && !StringUtils.isBlank(options) && !options.contains(IActionCommandResultSupplier.actionFieldName) ) {
            return options+String.format(",%s:%s", IActionCommandResultSupplier.actionFieldName, "Action");
        }
        return options;
    }

    private String getDefaultArgs() {
        var keySuffix = "output."+recordWriterFactory.toString()+".args";
        var defaultOptions = messageResolver.getMessageString(keySuffix);
        // TODO Only add headers for default options (i.e., apply here), 
        //      or also for user-supplied options (i.e., apply in resolveOptions())?
        //      If we change to the latter, we need to make header human-readble 
        //      formatting optional in addHeader()
        return addHeaders(defaultOptions);
    }
    
    private String addHeaders(String options) {
        if ( options==null ) { return null; }
        var result = new ArrayList<String>();
        for ( var o : options.split("[\\s,]+") ) {
            result.add(addHeader(o));
        }
        return String.join(",", result);
    }
    
    private String addHeader(String option) {
        if ( option.contains("=") ) { return option; } // Already contains header
        var keySuffix = "output."+recordWriterFactory.toString()+".header."+option;
        var header = messageResolver.getMessageString(keySuffix);
        if ( StringUtils.isBlank(header) ) {
            // For now, this method is only invoked for default options read from the resource
            // bundle, and resource bundles currently only contain default options for table
            // output, so we can unconditionally format as a human-readable string. However,
            // if any of the above changes, we may need to apply this formatting only for table
            // output for example.
            header = PropertyPathFormatter.humanReadable(option.replaceAll("String$", ""));
        }
        return String.format("%s:%s", option, header);
    }
}
