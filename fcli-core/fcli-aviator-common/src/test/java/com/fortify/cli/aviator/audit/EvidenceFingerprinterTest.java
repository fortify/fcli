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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.utils.FileUtils;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * The expected values are produced by an independent reference implementation of EVIDENCE_HASH_SPEC.md
 * (ref_evidence_hash.py), so these tests check the Java code against the specification, not against itself.
 */
class EvidenceFingerprinterTest {
    private static final String V1_BASIC = "v1:0db51319abef8b1af41c51a2cc224bcb0609d2be3a198f9de9f7c64e9a76f243";
    private static final String V4_CHANGE_INSIDE_WINDOW = "v1:6f5eee4499c6addaf9504574980bb7270fe39c1247d5ff34ff76b6e4edf3ca88";
    private static final String V7_TWO_TRACES_INNER = "v1:201f9200460af634ec13f957e786e31fbd13d05fc9cd271d2de2abbf18f4ecf1";
    private static final String V9_LINE_ZERO = "v1:b9cea585a80074bd34e7ab3b62f936e3c2c3436441155bb229b5b16573a3cf38";
    private static final String V10_UNICODE = "v1:5161604aff0fa18c220670c8cc805ba8becd8b80c3e4b17e8b3252cbb32529a3";
    private static final String V11_NEAR_END = "v1:9df994232b48540d93e0863b83e2e7db4fc016c13262b7c822b46f64e12419b5";
    private static final String V12_TRAILING_BLANK_LINES = "v1:d6b25c74959b308db176dbaaf57780291f274b29d34caa315127646b40f81f05";

    private Path fprPath;
    private FprHandle fprHandle;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (fprPath != null) {
            Files.deleteIfExists(fprPath);
        }
    }

    @Test
    void basicWindowMatchesReferenceVector() throws Exception {
        assertEquals(Optional.of(V1_BASIC), fingerprint(files("Main.java", numbered("line", 60)), trace(el("Main.java", 30))));
    }

    @Test
    void lineEndingsAndTrailingWhitespaceDoNotChangeTheFingerprint() throws Exception {
        String main = numbered("line", 60);
        assertEquals(Optional.of(V1_BASIC), fingerprint(files("Main.java", main.replace("\n", "\r\n")), trace(el("Main.java", 30))));
        String spaced = main.replace("line 10\n", "line 10   \t\n").replace("line 40\n", "line 40 \n");
        assertEquals(Optional.of(V1_BASIC), fingerprint(files("Main.java", spaced), trace(el("Main.java", 30))));
    }

    @Test
    void changeInsideTheWindowChangesTheFingerprint() throws Exception {
        String changed = numbered("line", 60).replace("line 31\n", "changed\n");
        assertEquals(Optional.of(V4_CHANGE_INSIDE_WINDOW), fingerprint(files("Main.java", changed), trace(el("Main.java", 30))));
    }

    @Test
    void changeOutsideTheWindowDoesNotChangeTheFingerprint() throws Exception {
        String changed = numbered("line", 100).replace("line 80\n", "far away change\n");
        assertEquals(Optional.of(V1_BASIC), fingerprint(files("Main.java", changed), trace(el("Main.java", 30))));
    }

    @Test
    void insertingLinesAboveTheTraceLineDoesNotChangeTheFingerprint() throws Exception {
        String shifted = "new a\nnew b\nnew c\n" + numbered("line", 60);
        assertEquals(Optional.of(V1_BASIC), fingerprint(files("Main.java", shifted), trace(el("Main.java", 33))));
    }

    @Test
    void multipleTracesAndInnerTracesMatchReferenceVector() throws Exception {
        StackTraceElement outer = el("Main.java", 30);
        outer.setInnerStackTrace(List.of(el("Util.java", 5)));
        Map<String, String> files = files("Main.java", numbered("line", 60));
        files.put("Util.java", numbered("util", 10));

        Optional<String> result = fingerprint(files, List.of(List.of(outer, el("Util.java", 2)), List.of(el("Main.java", 58))));

        assertEquals(Optional.of(V7_TWO_TRACES_INNER), result);
    }

    @Test
    void elementsOutsideTheSourceArchiveAreSkipped() throws Exception {
        Optional<String> result = fingerprint(files("Main.java", numbered("line", 60)),
                trace(el("Missing.java", 3), el("Main.java", 30)));

        assertEquals(Optional.of(V1_BASIC), result);
    }

    @Test
    void lineZeroUsesTheNoLineMarker() throws Exception {
        assertEquals(Optional.of(V9_LINE_ZERO), fingerprint(files("Main.java", numbered("line", 60)), trace(el("Main.java", 0))));
    }

    @Test
    void windowNearTheEndOfTheFileIsClamped() throws Exception {
        assertEquals(Optional.of(V11_NEAR_END), fingerprint(files("Main.java", numbered("line", 60)), trace(el("Main.java", 58))));
    }

    @Test
    void unicodeContentAndLeadingBomMatchReferenceVector() throws Exception {
        assertEquals(Optional.of(V10_UNICODE), fingerprint(files("Main.java", "café ünï\nsecond line\n"), trace(el("Main.java", 1))));
        assertEquals(Optional.of(V10_UNICODE), fingerprint(files("Main.java", "\uFEFFcafé ünï\nsecond line\n"), trace(el("Main.java", 1))));
    }

    @Test
    void trailingBlankLinesAreIgnored() throws Exception {
        assertEquals(Optional.of(V12_TRAILING_BLANK_LINES), fingerprint(files("Main.java", "l1\nl2\n\n\n"), trace(el("Main.java", 2))));
        assertEquals(Optional.of(V12_TRAILING_BLANK_LINES), fingerprint(files("Main.java", "l1\nl2\n"), trace(el("Main.java", 2))));
    }

    @Test
    void noUsableEvidenceYieldsNoFingerprint() throws Exception {
        assertEquals(Optional.empty(), fingerprint(files("Main.java", numbered("line", 60)), trace(el("Missing.java", 3))));
        assertEquals(Optional.empty(), fingerprint(files("Main.java", numbered("line", 60)), List.of()));
        assertEquals(Optional.empty(), fingerprint(files("Main.java", numbered("line", 60)), null));
    }

    @Test
    void sourceFileInTheArchiveIndexButUnreadableYieldsNoFingerprint() throws Exception {
        createFpr(Map.of(), Map.of("Gone.java", "src-archive/Gone.java"));

        Optional<String> result = new EvidenceFingerprinter(fprHandle, new FileUtils())
                .compute(trace(el("Gone.java", 3)));

        assertEquals(Optional.empty(), result);
    }

    @Test
    void fingerprintsCarryTheVersionPrefix() {
        assertTrue(EvidenceFingerprinter.isCurrentVersion(V1_BASIC));
        assertFalse(EvidenceFingerprinter.isCurrentVersion("v2:abc"));
        assertFalse(EvidenceFingerprinter.isCurrentVersion("abc"));
        assertFalse(EvidenceFingerprinter.isCurrentVersion(""));
        assertFalse(EvidenceFingerprinter.isCurrentVersion(null));
        assertNotEquals(V1_BASIC, V4_CHANGE_INSIDE_WINDOW);
    }

    private Optional<String> fingerprint(Map<String, String> files, List<List<StackTraceElement>> traces) throws Exception {
        Map<String, String> index = new LinkedHashMap<>();
        files.keySet().forEach(name -> index.put(name, "src-archive/" + name));
        createFpr(files, index);
        return new EvidenceFingerprinter(fprHandle, new FileUtils()).compute(traces);
    }

    private void createFpr(Map<String, String> files, Map<String, String> index) throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
            Files.deleteIfExists(fprPath);
        }
        fprPath = Files.createTempFile("evidence-fingerprint", ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            StringBuilder indexXml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><index>");
            index.forEach((key, internal) -> indexXml.append("<entry key=\"").append(key).append("\">")
                    .append(internal).append("</entry>"));
            indexXml.append("</index>");
            zip.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zip.write(indexXml.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (Map.Entry<String, String> file : files.entrySet()) {
                zip.putNextEntry(new ZipEntry("src-archive/" + file.getKey()));
                zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        fprHandle = new FprHandle(fprPath);
    }

    private static Map<String, String> files(String name, String content) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(name, content);
        return files;
    }

    private static List<List<StackTraceElement>> trace(StackTraceElement... elements) {
        return List.of(List.of(elements));
    }

    private static StackTraceElement el(String file, int line) {
        return StackTraceElement.builder().filename(file).line(line).build();
    }

    private static String numbered(String prefix, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append(prefix).append(' ').append(i).append('\n');
        }
        return sb.toString();
    }
}
