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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.DecodeResult;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.SourceDecodeException;
import com.fortify.cli.aviator.fpr.utils.SourceEncoder.SourceEncodeException;

/**
 * Behavior tests for source encoding factory + decode/encode.
 * Prefer real byte decode outcomes over describe()-only checks.
 */
class SourceDecodersTest {

    @Test
    void fromToken_rejectsBlankAndUnknownCharset() {
        assertThrows(IllegalArgumentException.class, () -> SourceDecoders.fromToken("  "));
        assertThrows(UnsupportedCharsetException.class, () -> SourceDecoders.fromToken("NOT-A-CHARSET"));
    }

    @Test
    void defaults_matchDocumentedCandidateOrder() {
        assertEquals("FPR,UTF-8,CP850", SourceDecoders.DEFAULT_SOURCE_ENCODINGS);
        assertEquals(SourceDecoders.DEFAULT_SOURCE_ENCODINGS, SourceDecoders.defaults().describe());
    }

    @Test
    void decode_fallsThroughFprWhenMetadataMissing() {
        byte[] utf8 = "hello".getBytes(StandardCharsets.UTF_8);
        DecodeResult result = SourceDecoders.defaults().decode(utf8, "Main.java", null);
        assertEquals("hello", result.content());
        assertEquals(StandardCharsets.UTF_8, result.charset());
        assertEquals("UTF-8", result.source());
    }

    @Test
    void decode_usesFprEncodingWhenMetadataPresent() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileEncoding("src/Main.java", "ISO-8859-1");
        byte[] latin1 = "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1);

        DecodeResult result = SourceDecoders.defaults().decode(latin1, "src/Main.java", metadata);

        assertEquals("caf\u00e9", result.content());
        assertEquals(StandardCharsets.ISO_8859_1, result.charset());
        assertTrue(result.source().startsWith("FPR("), result.source());
    }

    @Test
    void decode_usesWindows1252FromFprMetadata() {
        FVDLMetadata metadata = new FVDLMetadata();
        metadata.registerSourceFileEncoding("payments.c", "windows-1252");
        byte[] windows1252 = new byte[] {(byte) 0x93, 'p', 'r', 'e', 'm', 'i', 'u', 'm', (byte) 0x94};

        DecodeResult result = SourceDecoders.defaults().decode(windows1252, "payments.c", metadata);

        assertEquals("\u201cpremium\u201d", result.content());
        assertEquals("windows-1252", result.charset().name());
        assertEquals("FPR(windows-1252)", result.source());
    }

    @Test
    void decode_allCandidatesFail_messageListsAttempts() {
        ISourceDecoder decoder = SourceDecoders.fromCsv("UTF-8,US-ASCII");
        byte[] invalid = new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0x00};

        SourceDecodeException ex = assertThrows(SourceDecodeException.class,
                () -> decoder.decode(invalid, "bad.bin", null));

        assertTrue(ex.getMessage().contains("bad.bin"), ex.getMessage());
        assertTrue(ex.getMessage().contains("UTF-8"), ex.getMessage());
        assertTrue(ex.getMessage().contains("US-ASCII"), ex.getMessage());
    }

    @Test
    void encode_roundTripAndRejectsUnmappable() {
        String content = "secure code";
        byte[] encoded = SourceEncoder.encode(content, StandardCharsets.UTF_8, "a.java");
        assertEquals(content, SourceDecoders.fromToken("UTF-8").decode(encoded, "a.java", null).content());

        assertThrows(SourceEncodeException.class,
                () -> SourceEncoder.encode("caf\u00e9", StandardCharsets.US_ASCII, "a.java"));
    }

    @Test
    void fromToken_fprIsCaseInsensitive() {
        assertEquals("FPR", SourceDecoders.fromToken("fpr").describe());
    }

    @Test
    void fromCsv_compositesMultipleCandidates() {
        assertEquals("UTF-8,CP850", SourceDecoders.fromCsv("UTF-8,CP850").describe());
        assertEquals("UTF-8", SourceDecoders.fromCsv("UTF-8").describe());
    }
}
