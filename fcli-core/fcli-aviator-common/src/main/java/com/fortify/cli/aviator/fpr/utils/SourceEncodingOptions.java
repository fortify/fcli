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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

public final class SourceEncodingOptions {
    public static final String FPR_ENCODING = "FPR";
    public static final String DEFAULT_SOURCE_ENCODINGS = "FPR,UTF-8,CP850";

    private final List<String> candidates;

    private SourceEncodingOptions(List<String> candidates) {
        this.candidates = List.copyOf(candidates);
    }

    public static SourceEncodingOptions defaults() {
        return parse(DEFAULT_SOURCE_ENCODINGS);
    }

    public static SourceEncodingOptions parse(String value) {
        String effectiveValue = value == null || value.isBlank() ? DEFAULT_SOURCE_ENCODINGS : value;
        List<String> candidates = new ArrayList<>();
        for (String candidate : effectiveValue.split(",")) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                candidates.add(trimmed);
            }
        }
        if (candidates.isEmpty()) {
            candidates.add(FPR_ENCODING);
            candidates.add("UTF-8");
            candidates.add("CP850");
        }
        return new SourceEncodingOptions(candidates);
    }

    public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata fvdlMetadata) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        List<String> failures = new ArrayList<>();
        for (String candidate : candidates) {
            Optional<Charset> charset = resolveCharset(candidate, filename, fvdlMetadata, failures);
            if (charset.isEmpty()) {
                continue;
            }
            try {
                String content = charset.get().newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes))
                        .toString();
                return new DecodeResult(content, charset.get(), displayName(candidate, charset.get()));
            } catch (CharacterCodingException e) {
                failures.add(displayName(candidate, charset.get()) + " failed to decode source bytes");
            }
        }
        throw new SourceDecodeException("Could not decode source file '" + filename + "' using source encodings " + describe() +
                "; attempted: " + String.join("; ", failures));
    }

    public byte[] encode(String content, Charset charset, String filename) {
        try {
            ByteBuffer buffer = charset.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(content));
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        } catch (CharacterCodingException e) {
            throw new SourceEncodeException("Source file '" + filename + "' cannot be encoded using " + charset.name(), e);
        }
    }

    public String describe() {
        return String.join(",", candidates);
    }

    private Optional<Charset> resolveCharset(String candidate, String filename, FVDLMetadata fvdlMetadata, List<String> failures) {
        String encodingName = candidate;
        if (FPR_ENCODING.equalsIgnoreCase(candidate)) {
            if (fvdlMetadata == null) {
                failures.add("FPR metadata unavailable");
                return Optional.empty();
            }
            encodingName = fvdlMetadata.findSourceFileEncodingForFileName(filename);
            if (encodingName == null || encodingName.isBlank()) {
                failures.add("FPR encoding missing for '" + filename + "'");
                return Optional.empty();
            }
        }

        try {
            return Optional.of(Charset.forName(encodingName));
        } catch (Exception e) {
            failures.add(candidate + " resolved to unsupported encoding '" + encodingName + "'");
            return Optional.empty();
        }
    }

    private String displayName(String candidate, Charset charset) {
        return FPR_ENCODING.equalsIgnoreCase(candidate) ? FPR_ENCODING + "(" + charset.name() + ")" : charset.name();
    }

    public record DecodeResult(String content, Charset charset, String source) {}

    public static class SourceDecodeException extends AviatorSimpleException {
        private static final long serialVersionUID = 1L;

        public SourceDecodeException(String message) {
            super(message);
        }
    }

    public static class SourceEncodeException extends AviatorSimpleException {
        private static final long serialVersionUID = 1L;

        public SourceEncodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}