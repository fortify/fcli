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

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

/**
 * Strict source encode/decode helpers ({@link CodingErrorAction#REPORT}).
 * Public {@link #encode} is for write-back; package-private decode is shared by decoders.
 */
public final class SourceEncoder {
    private SourceEncoder() {}

    public static byte[] encode(String content, Charset charset, String filename) {
        try {
            return encodeStrict(content, charset);
        } catch (CharacterCodingException e) {
            throw new SourceEncodeException(
                    "Source file '" + filename + "' cannot be encoded using " + charset.name(), e);
        }
    }

    static String decodeStrict(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static byte[] encodeStrict(String content, Charset charset) throws CharacterCodingException {
        ByteBuffer buffer = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(content));
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    public static class SourceEncodeException extends AviatorSimpleException {
        private static final long serialVersionUID = 1L;

        public SourceEncodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
