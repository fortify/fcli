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
package com.fortify.cli.aviator.fpr.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

/** Tries candidates in order. Created via {@link SourceDecoders#of}. */
final class CompositeSourceDecoder implements ISourceDecoder {
    private final List<ISourceDecoder> decoders;

    CompositeSourceDecoder(List<? extends ISourceDecoder> decoders) {
        Objects.requireNonNull(decoders, "decoders must not be null");
        if (decoders.isEmpty()) {
            throw new IllegalArgumentException("decoders must not be empty");
        }
        this.decoders = List.copyOf(decoders);
    }

    @Override
    public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata fvdlMetadata) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        List<String> failures = new ArrayList<>();
        for (ISourceDecoder decoder : decoders) {
            try {
                return decoder.decode(bytes, filename, fvdlMetadata);
            } catch (SourceDecodeException e) {
                failures.add(e.getMessage() != null ? e.getMessage() : decoder.describe() + " failed");
            }
        }
        throw new SourceDecodeException("Could not decode source file '" + filename + "' using source encodings "
                + describe() + "; attempted: " + String.join("; ", failures));
    }

    @Override
    public String describe() {
        return decoders.stream().map(ISourceDecoder::describe).collect(Collectors.joining(","));
    }
}
