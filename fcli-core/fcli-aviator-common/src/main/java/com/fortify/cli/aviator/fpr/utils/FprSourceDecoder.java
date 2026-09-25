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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.Objects;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

/** Resolves encoding from FVDL metadata, then strict-decodes. Created via {@link SourceDecoders}. */
final class FprSourceDecoder implements ISourceDecoder {
    static final String TOKEN = "FPR";

    @Override
    public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata fvdlMetadata) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        Charset charset = resolveCharset(filename, fvdlMetadata);
        try {
            String content = SourceEncoder.decodeStrict(bytes, charset);
            return new DecodeResult(content, charset, TOKEN + "(" + charset.name() + ")");
        } catch (CharacterCodingException e) {
            throw new SourceDecodeException(TOKEN + "(" + charset.name() + ") failed to decode source bytes", e);
        }
    }

    private static Charset resolveCharset(String filename, FVDLMetadata fvdlMetadata) {
        if (fvdlMetadata == null) {
            throw new SourceDecodeException("FPR metadata unavailable");
        }
        String encodingName = fvdlMetadata.findSourceFileEncodingForFileName(filename);
        if (encodingName == null || encodingName.isBlank()) {
            throw new SourceDecodeException("FPR encoding missing for '" + filename + "'");
        }
        try {
            return Charset.forName(encodingName);
        } catch (Exception e) {
            throw new SourceDecodeException(
                    TOKEN + " resolved to unsupported encoding '" + encodingName + "'", e);
        }
    }

    @Override
    public String describe() {
        return TOKEN;
    }
}
