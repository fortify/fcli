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
package com.fortify.cli.aviator.ssc.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.fpr.util.FprHandle;

public final class AviatorSSCFprFilterHelper {
    private static final String WEBINSPECT_XML = "webinspect.xml";
    private static final String AUDIT_FVDL = "audit.fvdl";
    private static final Set<String> DAST_ENTRIES = Set.of(
        "VERSION", WEBINSPECT_XML, "audit.xml", "attachments.xml");

    private AviatorSSCFprFilterHelper() {}

    public enum Engine {
        SAST, DAST
    }

    public static void filterEngineSpecificFprInPlace(Path fpr, Engine engine) {
        try (FprHandle fprHandle = new FprHandle(fpr)) {
            Path root = fprHandle.getPath("/");
            validateRequiredEntry(root, engine);

            if (engine == Engine.SAST) {
                Files.deleteIfExists(fprHandle.getPath("/webinspect.xml"));
            } else {
                List<Path> entriesToDelete;
                try (Stream<Path> entries = Files.walk(root)) {
                    entriesToDelete = entries
                            .filter(path -> !path.equals(root))
                            .filter(path -> !shouldRetainDastEntry(path))
                            .sorted(Comparator.comparingInt(Path::getNameCount).reversed())
                            .toList();
                }
                for (Path entry : entriesToDelete) {
                    Files.deleteIfExists(entry);
                }
            }
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to filter FPR for " + engine + " upload", e);
        }
    }

    private static boolean shouldRetainDastEntry(Path path) {
        return path.getNameCount() == 1
            && DAST_ENTRIES.contains(path.getFileName().toString())
            && Files.isRegularFile(path);
    }

    private static void validateRequiredEntry(Path root, Engine engine) {
        String requiredEntry = engine == Engine.SAST ? AUDIT_FVDL : WEBINSPECT_XML;
        if (!Files.isRegularFile(root.resolve(requiredEntry))) {
            throw new FcliSimpleException(
                "FPR does not contain the required " + engine + " engine payload: " + requiredEntry);
        }
    }
}
