package com.fortify.cli.aviator.util;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class FPRLoadingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FPRLoadingUtil.class);

    private FPRLoadingUtil() {}

    private static final Pattern AUDIT_FVDL_PATTERN = Pattern.compile("audit\\.fvdl");
    private static final Pattern SRC_FILE_PATTERN = Pattern.compile("src-archive/(?!index\\.xml|ScanUUID).+");
    private static final Pattern REMEDIATION_FILE_PATTERN = Pattern.compile("remediations\\.xml");

    /**
     * Validates a given FPR file to ensure it meets the requirements for processing.
     * This includes checking for the existence of 'audit.fvdl', source code files,
     * and ensuring it's a valid SAST scan result.
     *
     * @param fprFile The FPR file to validate.
     * @throws AviatorSimpleException if the FPR is invalid due to missing required data.
     * @throws AviatorTechnicalException if the FPR file cannot be read or is corrupted.
     */
    public static void validateFpr(File fprFile) throws AviatorSimpleException, AviatorTechnicalException {
        if (fprFile == null || !fprFile.exists() || !fprFile.isFile()) {
            throw new AviatorSimpleException("FPR file not found or is a directory: " + (fprFile != null ? fprFile.getPath() : "null"));
        }

        try (ZipFile zipFile = new ZipFile(fprFile)) {
            boolean hasAuditFvdl = zipFile.stream()
                    .anyMatch(entry -> AUDIT_FVDL_PATTERN.matcher(entry.getName()).matches());

            if (!hasAuditFvdl) {
                boolean hasWebinspectXml = zipFile.stream()
                        .anyMatch(entry -> "webinspect.xml".equalsIgnoreCase(entry.getName()));
                if (hasWebinspectXml) {
                    throw new AviatorSimpleException("Invalid FPR: The provided fpr lacks the necessary SAST data. Fortify Aviator requires an FPR generated from a SAST scan.");
                }
                throw new AviatorSimpleException("Invalid FPR: The file does not contain 'audit.fvdl' in its root directory.");
            }

            if (!hasSource(fprFile)) {
                // The hasSource method logs the specific error, so we just throw a general exception.
                throw new AviatorSimpleException("Invalid FPR: Source code is missing or incomplete in the 'src-archive' directory.");
            }

            LOG.info("FPR validation successful for: {}", fprFile.getPath());

        } catch (IOException e) {
            LOG.error("Error reading or processing FPR file: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Cannot read FPR file. It may be corrupted or inaccessible.", e);
        }
    }

    /**
     * Checks if the FPR file contains source code by looking for 'src-archive/index.xml'
     * and at least one other file within the 'src-archive' directory.
     *
     * @param fprFile The FPR file to check.
     * @return true if source code is present, false otherwise.
     * @throws AviatorTechnicalException if there's an error reading the file.
     */
    public static boolean hasSource(File fprFile) throws AviatorTechnicalException {
        boolean foundIndex = false;
        boolean foundSourceFile = false;

        try (ZipFile zipFile = new ZipFile(fprFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String entryName = entry.getName().replace('\\', '/');
                    if ("src-archive/index.xml".equals(entryName)) {
                        foundIndex = true;
                        LOG.debug("Found src-archive/index.xml");
                    } else if (SRC_FILE_PATTERN.matcher(entryName).matches()) {
                        foundSourceFile = true;
                        LOG.debug("Found source file: {}", entryName);
                    }
                }
                // Optimization: if both are found, no need to continue scanning the zip.
                if (foundIndex && foundSourceFile) {
                    break;
                }
            }
        } catch (IOException e) {
            LOG.error("Error accessing FPR file to check for source: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Cannot read FPR file to check for source. It may be corrupted or inaccessible.", e);
        }

        if (!foundIndex) {
            LOG.warn("FPR is missing 'src-archive/index.xml'. Source code may not be included correctly. File: {}", fprFile.getPath());
        }
        if (!foundSourceFile) {
            LOG.warn("No source code files were found inside the 'src-archive' directory. File: {}", fprFile.getPath());
        }

        return foundIndex && foundSourceFile;
    }

    /**
     * Checks if the FPR file contains a 'remediations.xml' file.
     *
     * @param fprFile The FPR file to check.
     * @return true if the remediations file is found, false otherwise.
     * @throws AviatorTechnicalException if there's an error reading the file.
     */
    public static boolean hasRemediations(File fprFile) throws AviatorTechnicalException {
        try (ZipFile zipFile = new ZipFile(fprFile)) {
            return zipFile.stream()
                    .anyMatch(entry -> REMEDIATION_FILE_PATTERN.matcher(entry.getName()).matches());
        } catch (IOException e) {
            LOG.error("Error accessing FPR file to check for remediations: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Cannot read FPR file to check for remediations. It may be corrupted or inaccessible.", e);
        }
    }
}