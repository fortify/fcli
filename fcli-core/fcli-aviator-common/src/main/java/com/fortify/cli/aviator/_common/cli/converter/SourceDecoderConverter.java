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
package com.fortify.cli.aviator._common.cli.converter;

import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.TypeConversionException;

/**
 * Picocli adapter: maps a single {@code --source-encodings} token to an
 * {@link ISourceDecoder} via the domain factory {@link SourceDecoders}.
 */
public final class SourceDecoderConverter implements ITypeConverter<ISourceDecoder> {
    @Override
    public ISourceDecoder convert(String value) {
        try {
            return SourceDecoders.fromToken(value);
        } catch (IllegalArgumentException e) {
            // Covers blank tokens, IllegalCharsetNameException, UnsupportedCharsetException
            throw new TypeConversionException(
                    e.getMessage() != null ? e.getMessage() : "Invalid source encoding '" + value + "'");
        }
    }
}
