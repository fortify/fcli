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