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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.fortify.cli.common.exception.FcliTechnicalException;

/**
 * Utility class for gzip compression operations.
 * 
 * @author rsenden
 */
public class GzipHelper {
    
    /**
     * Compress a string using gzip and encode the result as Base64.
     * This is commonly required by APIs like GitHub Code Scanning SARIF upload.
     * 
     * @param content String content to compress
     * @return Base64-encoded gzip-compressed content
     */
    public static final String gzipAndBase64(String content) {
        try {
            byte[] compressed = gzip(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(compressed);
        } catch (IOException e) {
            throw new FcliTechnicalException("Error compressing content", e);
        }
    }
    
    /**
     * Compress byte array using gzip.
     * 
     * @param data Byte array to compress
     * @return Gzip-compressed byte array
     * @throws IOException If compression fails
     */
    public static final byte[] gzip(byte[] data) throws IOException {
        var baos = new ByteArrayOutputStream();
        try (var gzipOut = new GzipCompressorOutputStream(baos)) {
            gzipOut.write(data);
        }
        return baos.toByteArray();
    }
}
