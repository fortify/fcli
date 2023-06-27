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
package com.fortify.cli.common.progress.cli.mixin;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.common.progress.helper.ProgressWriterType;

import picocli.CommandLine.ITypeConverter;

public final class ProgressWriterTypeConverter implements ITypeConverter<ProgressWriterType> {
    @Override
    public ProgressWriterType convert(String value) throws Exception {
        return ProgressWriterType.valueOf(value.replace('-', '_'));
    }
    
    public static final class ProgressWriterTypeIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public ProgressWriterTypeIterable() { 
            super(Stream.of(ProgressWriterType.values())
                    .map(Enum::name)
                    .map(s->s.replace('_', '-'))
                    .collect(Collectors.toList())); 
        }
    }
}