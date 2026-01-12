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
package com.fortify.cli.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GzipHelperTest {
    
    @Test
    @DisplayName("gzipAndBase64 returns non-null Base64 string")
    void testGzipAndBase64ReturnsNonNull() {
        var result = GzipHelper.gzipAndBase64("test content");
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }
    
    @Test
    @DisplayName("gzipAndBase64 produces valid Base64")
    void testGzipAndBase64ProducesValidBase64() {
        var result = GzipHelper.gzipAndBase64("test content");
        // Should not throw exception when decoding
        byte[] decoded = Base64.getDecoder().decode(result);
        assertNotNull(decoded);
        assertTrue(decoded.length > 0);
    }
    
    @Test
    @DisplayName("gzipAndBase64 produces decompressible gzip content")
    void testGzipAndBase64ProducesDecompressibleContent() throws IOException {
        var original = "Hello, this is a test message for gzip compression!";
        var compressed = GzipHelper.gzipAndBase64(original);
        
        // Decode Base64 and decompress
        byte[] compressedBytes = Base64.getDecoder().decode(compressed);
        try (var gzipIn = new GzipCompressorInputStream(new ByteArrayInputStream(compressedBytes))) {
            var decompressed = new String(gzipIn.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(original, decompressed);
        }
    }
    
    @Test
    @DisplayName("gzip compresses byte array")
    void testGzipCompressesByteArray() throws IOException {
        var original = "This is a longer test message that should compress well when using gzip compression algorithm.".getBytes(StandardCharsets.UTF_8);
        var compressed = GzipHelper.gzip(original);
        
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        
        // Verify it can be decompressed back to original
        try (var gzipIn = new GzipCompressorInputStream(new ByteArrayInputStream(compressed))) {
            var decompressed = gzipIn.readAllBytes();
            assertEquals(original.length, decompressed.length);
        }
    }
    
    @Test
    @DisplayName("gzip handles empty string")
    void testGzipHandlesEmptyString() {
        var result = GzipHelper.gzipAndBase64("");
        assertNotNull(result);
        // Even empty content produces valid gzip header in Base64
        assertTrue(result.length() > 0);
    }
}
