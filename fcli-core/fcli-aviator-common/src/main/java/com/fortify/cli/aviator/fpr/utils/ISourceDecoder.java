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

import java.nio.charset.Charset;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;

/**
 * Decodes source file bytes using a specific encoding strategy.
 * Implementations may resolve the encoding from FVDL metadata, use a fixed
 * {@link Charset}, or try multiple candidate decoders in order.
 */
public interface ISourceDecoder {
    /**
     * Decode source file bytes into text.
     *
     * @param bytes        raw source file bytes
     * @param filename     source file name (used for FPR encoding lookup and error messages)
     * @param fvdlMetadata optional FVDL metadata; required only by FPR-based decoders
     * @return decoded content together with the charset that produced it
     * @throws SourceDecodeException if this decoder cannot decode the bytes
     */
    DecodeResult decode(byte[] bytes, String filename, FVDLMetadata fvdlMetadata);

    /**
     * Human-readable description of this decoder (used in CLI help and error messages).
     */
    String describe();

    record DecodeResult(String content, Charset charset, String source) {}

    class SourceDecodeException extends AviatorSimpleException {
        private static final long serialVersionUID = 1L;

        public SourceDecodeException(String message) {
            super(message);
        }

        public SourceDecodeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
