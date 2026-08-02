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

/**
 * Domain factory for {@link ISourceDecoder} instances. Single source of truth for
 * token/CSV parsing and default candidate order. CLI converters should delegate here.
 */
public final class SourceDecoders {
    public static final String DEFAULT_SOURCE_ENCODINGS = "FPR,UTF-8,CP850";

    private SourceDecoders() {}

    /**
     * Map a single token to a decoder: {@code FPR} or a charset name.
     *
     * @throws IllegalArgumentException if token is blank
     * @throws java.nio.charset.IllegalCharsetNameException if charset name is illegal
     * @throws java.nio.charset.UnsupportedCharsetException if charset is unsupported
     */
    public static ISourceDecoder fromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Source encoding must not be blank");
        }
        String trimmed = token.trim();
        if (FprSourceDecoder.TOKEN.equalsIgnoreCase(trimmed)) {
            return new FprSourceDecoder();
        }
        return new CharsetSourceDecoder(trimmed);
    }

    /**
     * Parse a comma-separated list of encoding tokens into a single decoder
     * (composite when more than one candidate).
     */
    public static ISourceDecoder fromCsv(String csv) {
        String effective = csv == null || csv.isBlank() ? DEFAULT_SOURCE_ENCODINGS : csv;
        List<ISourceDecoder> decoders = new ArrayList<>();
        for (String part : effective.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                decoders.add(fromToken(trimmed));
            }
        }
        if (decoders.isEmpty()) {
            return defaults();
        }
        return of(decoders);
    }

    /**
     * Compose an ordered candidate list into one decoder.
     */
    public static ISourceDecoder of(List<? extends ISourceDecoder> decoders) {
        Objects.requireNonNull(decoders, "decoders must not be null");
        if (decoders.isEmpty()) {
            return defaults();
        }
        if (decoders.size() == 1) {
            return decoders.get(0);
        }
        return new CompositeSourceDecoder(decoders);
    }

    public static ISourceDecoder defaults() {
        return fromCsv(DEFAULT_SOURCE_ENCODINGS);
    }
}
