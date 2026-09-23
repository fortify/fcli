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

/** Fixed-{@link Charset} decoder. Created via {@link SourceDecoders}. */
final class CharsetSourceDecoder implements ISourceDecoder {
    private final Charset charset;
    private final String label;

    CharsetSourceDecoder(String charsetName) {
        this.charset = Charset.forName(charsetName);
        this.label = charsetName;
    }

    @Override
    public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata fvdlMetadata) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        try {
            return new DecodeResult(SourceEncoder.decodeStrict(bytes, charset), charset, label);
        } catch (CharacterCodingException e) {
            throw new SourceDecodeException(label + " failed to decode source bytes", e);
        }
    }

    @Override
    public String describe() {
        return label;
    }
}
