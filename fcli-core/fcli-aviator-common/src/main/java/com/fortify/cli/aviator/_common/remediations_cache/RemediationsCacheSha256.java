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
package com.fortify.cli.aviator._common.remediations_cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fortify.cli.common.exception.FcliTechnicalException;

public final class RemediationsCacheSha256 {
    private RemediationsCacheSha256() {}

    public static String hashFile(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return hashStream(in);
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to compute SHA-256 for " + path, e);
        }
    }

    public static String hashStream(InputStream in) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Digest is updated by DigestInputStream
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new FcliTechnicalException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to compute SHA-256", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
