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
package com.fortify.cli.aviator._common.cli.mixin;

import java.util.List;

import com.fortify.cli.aviator._common.cli.converter.SourceDecoderConverter;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;

import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Shared {@code --source-encodings} option for Aviator commands that decode
 * (and optionally re-encode) source files from an FPR.
 */
public class SourceEncodingsMixin {
    @Getter
    @Option(names = {"--source-encodings"},
            split = ",",
            converter = SourceDecoderConverter.class,
            defaultValue = SourceDecoders.DEFAULT_SOURCE_ENCODINGS,
            paramLabel = "encoding",
            descriptionKey = "fcli.aviator.source-encodings")
    private List<ISourceDecoder> sourceDecoders;

    /**
     * Returns a single decoder that tries the configured candidates in order.
     */
    public ISourceDecoder getSourceDecoder() {
        return SourceDecoders.of(sourceDecoders);
    }
}
