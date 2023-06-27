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
package com.fortify.cli.common.output.writer.output.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import com.fortify.cli.common.output.OutputFormat;

import picocli.CommandLine.ITypeConverter;

public final class OutputFormatConfigConverter implements ITypeConverter<OutputFormatConfig> {
    @Override
    public OutputFormatConfig convert(String value) throws Exception {
        int pos = value.indexOf('=');
        String outputFormatString = pos==-1 ? value : value.substring(0, pos);
        String options = pos==-1 ? null : value.substring(pos+1);
        return new OutputFormatConfig(valueOfFormattedString(outputFormatString), options);
    }
    
    public static final String[] formattedValueStrings() {
        return Stream.of(OutputFormat.values())
                .map(OutputFormat::name)
                .map(s->s.replace('_', '-'))
                .toArray(String[]::new);
    }
    
    public static final OutputFormat valueOfFormattedString(String s) {
        return OutputFormat.valueOf(s.replace('-', '_'));
    }
    
    public static final class OutputFormatIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public OutputFormatIterable() { 
            super(Arrays.asList(formattedValueStrings())); 
        }
    }
}