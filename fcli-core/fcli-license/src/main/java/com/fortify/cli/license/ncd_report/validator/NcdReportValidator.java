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
package com.fortify.cli.license.ncd_report.validator;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.license.ncd_report.reader.NcdReportReader;

/**
 * Validates NCD report integrity, including checksum verification.
 */
public final class NcdReportValidator {
    private static final Pattern CHECKSUM_LINE_PATTERN = Pattern.compile("^([0-9a-fA-F]{64})\\s+\\*?(.+)$");

    /**
     * Validate checksums in an NCD report, comparing stored checksums against current file content.
     * @param reader NcdReportReader for the report to validate
     * @return List of validation errors (empty if all valid)
     * @throws FcliSimpleException if checksums.sha256 file not found or invalid format
     * @throws FcliTechnicalException if checksum computation fails
     */
    public static List<String> validateChecksums(NcdReportReader reader) {
        var errors = new ArrayList<String>();
        var entryPath = reader.entryPath("checksums.sha256");

        if ( !Files.exists(entryPath) ) {
            throw new FcliSimpleException("Report integrity check failed: checksums.sha256 not found in %s", reader.getReportPath());
        }

        try {
            var checksumsByFile = parseChecksumFile(entryPath);
            var reportPath = reader.getReportPath();

            for ( var entry : checksumsByFile.entrySet() ) {
                var fileName = entry.getKey();
                var expectedChecksum = entry.getValue();
                var filePath = reader.entryPath(fileName);

                if ( !Files.exists(filePath) ) {
                    errors.add(String.format("Missing file: %s", fileName));
                } else {
                    var actualChecksum = sha256(filePath);
                    if ( !actualChecksum.equals(expectedChecksum) ) {
                        errors.add(String.format("Checksum mismatch for %s: expected %s, got %s", 
                            fileName, expectedChecksum, actualChecksum));
                    }
                }
            }
        } catch ( FcliSimpleException | FcliTechnicalException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error validating checksums in %s", reader.getReportPath()), e);
        }

        return errors;
    }

    /**
     * Parse checksums.sha256 file and return map of filename to checksum.
     * Format: "HEXDIGEST *filename" or "HEXDIGEST filename"
     * @param checksumsPath Path to checksums.sha256 file
     * @return Map of filename to checksum
     * @throws FcliSimpleException if file format is invalid
     * @throws FcliTechnicalException if file read fails
     */
    private static Map<String, String> parseChecksumFile(java.nio.file.Path checksumsPath) {
        var result = new HashMap<String, String>();
        try {
            var lines = Files.readAllLines(checksumsPath, StandardCharsets.UTF_8);
            for ( var line : lines ) {
                if ( line.isBlank() ) {
                    continue;
                }
                var matcher = CHECKSUM_LINE_PATTERN.matcher(line);
                if ( !matcher.matches() ) {
                    throw new FcliSimpleException("Invalid line in checksums.sha256: %s", line);
                }
                var checksum = matcher.group(1);
                var fileName = matcher.group(2);
                result.put(fileName, checksum);
            }
            return result;
        } catch ( FcliSimpleException | FcliTechnicalException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error reading checksums.sha256 from %s", checksumsPath), e);
        }
    }

    /**
     * Compute SHA-256 checksum of a file.
     * @param path Path to file
     * @return Uppercase hex digest (64 characters)
     * @throws FcliBugException if SHA-256 not available
     * @throws FcliTechnicalException if file read fails
     */
    public static String sha256(java.nio.file.Path path) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path));
            return String.format("%064X", new BigInteger(1, hash));
        } catch ( NoSuchAlgorithmException e ) {
            throw new FcliBugException("SHA-256 not available", e);
        } catch ( Exception e ) {
            throw new FcliTechnicalException(String.format("Error calculating checksum for %s", path), e);
        }
    }

    private NcdReportValidator() {}
}
