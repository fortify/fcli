package com.fortify.cli.aviator.util;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public final class FPRLoadingUtil {

    private static final Logger LOG = LoggerFactory.getLogger(FPRLoadingUtil.class);

    private FPRLoadingUtil() {}

    private static final Pattern AUDIT_FVDL_PATTERN = Pattern.compile("audit\\.fvdl");
    private static final Pattern SRC_FILE_PATTERN = Pattern.compile("src-archive/(?!index\\.xml|ScanUUID).+");

    public static void validateFpr(File fprFile) throws AviatorSimpleException {
        if (!fprFile.exists() || !fprFile.isFile()) {
            throw new AviatorSimpleException("FPR file not found or is a directory: " + fprFile.getPath());
        }

        try (ZipFile zipFile = new ZipFile(fprFile)) {
            boolean hasAuditFvdl = zipFile.stream()
                    .anyMatch(entry -> AUDIT_FVDL_PATTERN.matcher(entry.getName()).matches());

            if (!hasAuditFvdl) {
                throw new AviatorSimpleException("Invalid FPR: The file does not contain 'audit.fvdl' in its root directory.");
            }

            boolean hasSourceIndex = zipFile.stream()
                    .anyMatch(entry -> "src-archive/index.xml".equals(entry.getName().replace('\\', '/')));

            if (!hasSourceIndex) {
                throw new AviatorSimpleException("Invalid FPR: The file is missing 'src-archive/index.xml'. Source code may not be included.");
            }

            boolean hasSourceFiles = zipFile.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(entry -> entry.getName().replace('\\', '/'))
                    .anyMatch(name -> SRC_FILE_PATTERN.matcher(name).matches());

            if (!hasSourceFiles) {
                throw new AviatorSimpleException("Invalid FPR: No source code files were found inside the 'src-archive' directory.");
            }

            LOG.info("FPR validation successful for: {}", fprFile.getPath());

        } catch (IOException e) {
            LOG.error("Error reading or processing FPR file: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Cannot read FPR file. It may be corrupted or inaccessible.", e);
        }
    }
}