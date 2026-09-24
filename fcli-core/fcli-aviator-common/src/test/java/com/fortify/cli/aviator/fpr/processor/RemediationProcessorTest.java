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
package com.fortify.cli.aviator.fpr.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.RemediationProcessingOptions;
import com.fortify.cli.aviator.fpr.remediation.RemediationProcessor;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.aviator.util.FprHandle;

class RemediationProcessorTest {
    private static final String REMEDIATIONS_NAMESPACE = "xmlns://www.fortify.com/schema/remediations";

    @TempDir
    Path tempDir;

    @Test
    void testIssueIdFilterAppliesOnlyRequestedRemediations() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "        oldTwo();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(remediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
                var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                    options(Set.of("ISSUE-2", "ISSUE-404"), RemediationExecutionMode.APPLY));
            var metric = processor.processRemediationXML();

            assertTrue(metric.isFiltered());
                assertEquals(RemediationExecutionMode.APPLY, metric.executionMode());
            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());
            assertEquals(Set.of("ISSUE-2"), metric.appliedIssueIds());
            assertEquals(Set.of("Example.java"), metric.modifiedFiles());
            assertEquals(1, metric.skippedByReason().get("Requested issue not found in remediations"));
            String updatedContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertTrue(updatedContent.contains("        oldOne();"));
            assertTrue(updatedContent.contains("        newTwo();"));
            assertFalse(updatedContent.contains("        newOne();"));
        }
    }

    @Test
    void testIssueIdFilterWithNoMatchesCountsRequestedIdsAsSkipped() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-no-match"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(singleRemediationXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
                var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                    options(new LinkedHashSet<>(Set.of("ISSUE-404", "ISSUE-405")), RemediationExecutionMode.APPLY));
            var metric = processor.processRemediationXML();

            assertTrue(metric.isFiltered());
            assertEquals(2, metric.totalRemediations());
            assertEquals(0, metric.appliedRemediations());
            assertEquals(2, metric.skippedRemediations());
            assertEquals(Set.of(), metric.appliedIssueIds());
            assertEquals(Set.of(), metric.modifiedFiles());
            assertEquals(2, metric.skippedByReason().get("Requested issue not found in remediations"));
            assertEquals(originalContent, Files.readString(sourceFile, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testUnfilteredPathTraversalCandidateIsSkippedWithoutAborting() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-path-traversal"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(pathTraversalAndValidRemediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString());
            var metric = processor.processRemediationXML();

            assertFalse(metric.isFiltered());
            assertEquals(RemediationExecutionMode.APPLY, metric.executionMode());
            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());
            assertEquals(1, metric.skippedByReason().get("Source file outside source directory"));
            String updatedContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertTrue(updatedContent.contains("        newOne();"));
        }
    }

    @Test
    void testLoadsFvdlMetadataFromZipBackedFprPath() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-zip-backed"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        Path fprPath = createFpr(singleRemediationXml(TestHashUtil.sha256Base64Unix(originalContent)));
        Path cachePath = tempDir.resolve("remediations-cache.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(cachePath))) {
            zipOutputStream.putNextEntry(new ZipEntry("fprs/001.fpr"));
            Files.copy(fprPath, zipOutputStream);
            zipOutputStream.closeEntry();
        }

        try (FileSystem cacheFileSystem = FileSystems.newFileSystem(cachePath, (ClassLoader) null);
                FprHandle fprHandle = new FprHandle(cacheFileSystem.getPath("/fprs/001.fpr"))) {
            var metric = new RemediationProcessor(fprHandle, sourceDir.toString()).processRemediationXML();

            assertEquals(1, metric.appliedRemediations());
            assertEquals(Set.of("Example.java"), metric.modifiedFiles());
            assertTrue(Files.readString(sourceFile, StandardCharsets.UTF_8).contains("        newOne();"));
        }
    }

    @Test
    void testLoadsDeclaredEncodingFromZipBackedFprPath() throws Exception {
        Charset sourceCharset = Charset.forName("windows-1252");
        String originalLine = "String price = \"€100\";";
        String replacementLine = "String price = \"EUR100\";";
        String originalContent = originalLine + "\n";
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-zip-encoding"));
        Path sourceFile = sourceDir.resolve("Example.java");
        Files.write(sourceFile, originalContent.getBytes(sourceCharset));

        String remediationsXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
                    <Remediation instanceId="ISSUE-ENCODING">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>1</LineFrom>
                                <LineTo>1</LineTo>
                                <Context before="0" after="0">%s</Context>
                                <OriginalCode>%s</OriginalCode>
                                <NewCode>%s</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(TestHashUtil.sha256Base64Unix(originalContent), originalLine, originalLine, replacementLine);
        Path fprPath = createFpr(remediationsXml, sourceCharset.name());
        Path cachePath = tempDir.resolve("remediations-encoding-cache.zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(cachePath))) {
            zipOutputStream.putNextEntry(new ZipEntry("fprs/001.fpr"));
            Files.copy(fprPath, zipOutputStream);
            zipOutputStream.closeEntry();
        }

        try (FileSystem cacheFileSystem = FileSystems.newFileSystem(cachePath, (ClassLoader) null);
                FprHandle fprHandle = new FprHandle(cacheFileSystem.getPath("/fprs/001.fpr"))) {
            var metric = new RemediationProcessor(fprHandle, sourceDir.toString()).processRemediationXML();

            assertEquals(1, metric.appliedRemediations());
            assertEquals(replacementLine + "\n", new String(Files.readAllBytes(sourceFile), sourceCharset));
        }
    }

    private Path createFpr(String remediationsXml) throws IOException {
        return createFpr(remediationsXml, "UTF-8");
    }

    private Path createFpr(String remediationsXml, String sourceEncoding) throws IOException {
        Path fprPath = tempDir.resolve("test.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            // Encoding metadata is required by RemediationProcessor (FVDL Build/SourceFiles).
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="%s">
                            <Name>Example.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """.formatted(sourceEncoding));
            writeEntry(zipOutputStream, "remediations.xml", remediationsXml);
        }
        return fprPath;
    }

    /**
     * Fix #2 (exact-line disambiguation): two identical context blocks are ambiguous on their
     * own, but the declared/projected LineFrom lands exactly on the first occurrence, so it
     * resolves deterministically instead of being skipped as ambiguous.
     */
    @Test
    void resolvesAmbiguousContextByExactDeclaredPosition() throws Exception {
        String originalSource = "before\nTARGET\nafter\nbefore\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("before\nREPLACED\nafter\nbefore\nTARGET\nafter\n", Files.readString(sourceFile));
    }

    /**
     * Fix #2 must not guess: when the declared/projected position doesn't exactly match any of
     * the ambiguous candidates, it still throws SOURCE_CONTEXT_AMBIGUOUS rather than picking one.
     */
    @Test
    void skipsAmbiguousContextWhenDeclaredPositionMatchesNoCandidate() throws Exception {
        String originalSource = "before\nTARGET\nafter\nbefore\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(99, 99, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.totalRemediations());
        assertEquals(0, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of(
          "Source context matched multiple locations", 1),
                metric.skippedByReason());
        assertEquals(originalSource, Files.readString(sourceFile));
    }
    @Test
    void appliesRemediationWhenContextMatchesOnce() throws Exception {
        Path sourceFile = writeSourceFile("before\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    @Test
    void appliesOriginalCodeAfterLeadingContextLines() throws Exception {
        Path sourceFile = writeSourceFile("TARGET\nkeep\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(3, 3, 2, 1, "TARGET\nkeep\nTARGET\nafter", "TARGET", "REPLACED");

      RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
          metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals("TARGET\nkeep\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

      @Test
      void appliesRemediationWhenContextStartsWithBlankLine() throws Exception {
        Path sourceFile = writeSourceFile("header\n\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(3, 3, 1, 1, "\nTARGET\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
          metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals("header\n\nREPLACED\nafter\n", Files.readString(sourceFile));
      }

    @Test
    void nestedRemediationWithDifferentContentIsPossiblyRemediated() throws Exception {
        Path sourceFile = writeSourceFile("before\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("wide-fix", 1, 3, 0, 0, "before\nTARGET\nafter",
                "before\nTARGET\nafter", "wideline1\nwideline2\nwideline3"),
            new RemediationSpec("narrow-fix", 2, 2, 1, 1, "before\ntarget\nafter",
                "TARGET", "NARROW_DIFFERENT")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.possiblyRemediatedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("wideline1\nwideline2\nwideline3\n", Files.readString(sourceFile));
    }

    /**
     * Fix #1 (boundary-token duplication): NewCode repeats the line immediately before LineFrom
     * verbatim as its first line; that duplicate must be dropped rather than doubling the line.
     */
    @Test
    void dropsDuplicatedLeadingBoundaryLineInNewCode() throws Exception {
        Path sourceFile = writeSourceFile("line1\nline2\nline3\n");
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "line1\nline2\nline3", "line2", "line1\nreplaced2");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("line1\nreplaced2\nline3\n", Files.readString(sourceFile));
    }

    /**
     * Fix #1 (boundary-token duplication): NewCode repeats the line immediately after LineTo
     * verbatim as its last line; that duplicate must be dropped rather than doubling the line.
     */
    @Test
    void dropsDuplicatedTrailingBoundaryLineInNewCode() throws Exception {
        Path sourceFile = writeSourceFile("line1\nline2\nline3\n");
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "line1\nline2\nline3", "line2", "replaced2\nline3");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("line1\nreplaced2\nline3\n", Files.readString(sourceFile));
    }

    /**
     * Regression test: the offset ledger must record the delta actually spliced into the file
     * (post boundary-dedup), not the raw NewCode line count. A prior hunk in this file drops a
     * duplicated boundary line (fix #1), shrinking the file by one more line than NewCode's raw
     * length implies. A later hunk in the same file targets the second of two identical "TARGET"
     * blocks; only a correctly-shifted projection lands exactly on it. With the pre-fix (buggy)
     * delta, the projected position misses, the fuzzy fallback's context match is ambiguous
     * between the two blocks, and neither lands on the declared/projected position either -
     * causing an incorrect skip instead of resolving to the second occurrence.
     */
    @Test
    void offsetLedgerAccountsForDedupWhenProjectingLaterHunkInSameFile() throws Exception {
        String originalSource = "line0\nhead1\nhead2\nhead3\nbefore\nTARGET\nafter\nbefore\nTARGET\nafter\ntail\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("hunkA", 2, 4, 1, 1, "line0\nhead1\nhead2\nhead3\nbefore",
                "head1\nhead2\nhead3", "line0\nreplacedHead"),
            new RemediationSpec("hunkB", 9, 9, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("line0\nreplacedHead\nbefore\nTARGET\nafter\nbefore\nREPLACED\nafter\ntail\n",
            Files.readString(sourceFile));
    }

    /**
     * Fix #1 (boundary-token duplication): NewCode repeats BOTH the line before LineFrom and the
     * line after LineTo verbatim in the same hunk. Both duplicates must be dropped, not just one -
     * dropDuplicatedBoundaryTokens checks leading and trailing independently, so this proves
     * neither check clobbers or is skipped because of the other having already mutated the list.
     */
    @Test
    void dropsBothDuplicatedBoundaryLinesWhenNewCodeRepeatsBothNeighbors() throws Exception {
        Path sourceFile = writeSourceFile("line1\nline2\nline3\n");
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "line1\nline2\nline3", "line2",
            "line1\nreplaced2\nline3");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("line1\nreplaced2\nline3\n", Files.readString(sourceFile));
    }

    /**
     * Fix #2 (exact-line disambiguation) must also cover OriginalCode search, not just Context
     * search: two identical single-line OriginalCode matches inside the context window are
     * ambiguous on their own, but the declared/projected LineFrom lands exactly on the second
     * occurrence, so it resolves deterministically instead of being skipped as ambiguous.
     */
    @Test
    void resolvesAmbiguousOriginalCodeByExactDeclaredPosition() throws Exception {
        String originalSource = "before\nTARGET\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(3, 3, 1, 1, "before\nTARGET\nTARGET\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("before\nTARGET\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * Companion to the above: when the declared/projected position matches none of the ambiguous
     * OriginalCode candidates, it must throw ORIGINAL_CODE_AMBIGUOUS rather than guessing one.
     */
    @Test
    void skipsAmbiguousOriginalCodeWhenDeclaredPositionMatchesNoCandidate() throws Exception {
        String originalSource = "before\nTARGET\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        Path fprPath = createRemediationFpr(99, 99, 1, 1, "before\nTARGET\nTARGET\nafter", "TARGET", "REPLACED");

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(0, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Original code matched multiple locations", 1), metric.skippedByReason());
        assertEquals(originalSource, Files.readString(sourceFile));
    }

    /**
     * Phase 2 "make the hash check work": when the declared Hash matches the canonical form of
     * the file content, the change is applied directly at the declared line range with NO context
     * or OriginalCode search at all. Context and OriginalCode here are deliberately garbage text
     * that appears nowhere in the source - if the hash-match fast path were not actually short-
     * circuiting the fuzzy search, this remediation would be skipped as not-found.
     */
    @Test
    void appliesRemediationViaCanonicalHashMatchWithoutContextOrOriginalCodeSearch() throws Exception {
        String originalSource = "before\nTARGET\nafter\n";
        Path sourceFile = writeSourceFile(originalSource);
        String canonicalHash = sha256Base64(FileUtil.canonicalizeForHash(originalSource)
            .getBytes(StandardCharsets.UTF_8));
        Path fprPath = createRemediationFprWithHash(2, 2, 1, 1,
            "garbage-context-not-in-file\nmore-garbage\nstill-garbage", "GARBAGE-CODE-NOT-IN-FILE",
            "REPLACED", canonicalHash);

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * SUPERSEDED: a broader prior fix's actually-written content already contains the narrower
     * candidate's proposed replacement (normalized). The narrower remediation must be recognized
     * as superseded and not re-applied/re-searched at all.
     */
    @Test
    void supersededRemediationWithMatchingContentIsNotReapplied() throws Exception {
        Path sourceFile = writeSourceFile("before\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("wide-fix", 1, 3, 0, 0, "before\nTARGET\nafter",
                "before\nTARGET\nafter", "before\nREPLACED\nafter"),
            new RemediationSpec("narrow-fix", 2, 2, 1, 1, "before\ntarget\nafter",
                "TARGET", "REPLACED")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.supersededRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * A nested multi-line candidate whose NewCode matches the wide fix still does not prove
     * SUPERSEDED: the classifier passes {@code comparisonCode} (all whitespace removed), and
     * {@code contentCovers} equals that against a newline-preserving slice. The narrow hunk is
     * POSSIBLY_REMEDIATED and is not re-applied.
     */
    @Test
    void multilineNestedMatchingContentIsPossiblyRemediatedNotReapplied() throws Exception {
        Path sourceFile = writeSourceFile("before\nline2\nline3\nafter\n");
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("wide-fix", 1, 4, 0, 0, "before\nline2\nline3\nafter",
                "before\nline2\nline3\nafter", "before\nREPLACED2\nREPLACED3\nafter"),
            new RemediationSpec("narrow-fix", 2, 3, 1, 1, "before\nline2\nline3\nafter",
                "line2\nline3", "REPLACED2\nREPLACED3")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.supersededRemediations());
        assertEquals(1, metric.possiblyRemediatedRemediations());
        assertEquals("before\nREPLACED2\nREPLACED3\nafter\n", Files.readString(sourceFile));
    }

    /**
     * CONFLICTS: two remediations with a partial, non-nested line overlap (neither range contains
     * the other) is genuinely ambiguous coverage. Fix #2's "no heuristic guessing" philosophy
     * extends here too: the later, overlapping remediation must be skipped with
     * CONFLICTS_WITH_ANOTHER_FIX rather than guessed at.
     */
    @Test
    void conflictingOverlappingRemediationIsSkippedNotGuessed() throws Exception {
        Path sourceFile = writeSourceFile("line1\nline2\nline3\nline4\nline5\n");
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("fix-A", 2, 3, 1, 1, "line1\nline2\nline3\nline4",
                "line2\nline3", "A2\nA3"),
            new RemediationSpec("fix-B", 3, 4, 1, 1, "line2\nline3\nline4\nline5",
                "line3\nline4", "B3\nB4")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Conflicts with another fix", 1), metric.skippedByReason());
        assertEquals("line1\nA2\nA3\nline4\nline5\n", Files.readString(sourceFile));
    }

    /**
     * Mixed outcome within a single remediation: one file-change is a valid APPLY candidate
     * while a sibling file-change in the SAME remediation conflicts with an already-applied
     * prior fix. Remediations apply atomically, so the whole remediation must be rejected as
     * CONFLICTS_WITH_ANOTHER_FIX as soon as any hunk is CONFLICTS, rather than falling through
     * to the applier's best-effort offset/fuzzy-anchor fallback for the conflicting hunk while
     * silently applying the valid one.
     */
    @Test
    void mixedOutcomeRemediationWithOneConflictingHunkIsSkippedEntirely() throws Exception {
        Path sharedFile = writeSourceFile("Shared.java", "s1\ns2\ns3\ns4\ns5\n");
        Path exampleFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");

        LinkedHashMap<String, List<FileChangeSpec>> remediations = new LinkedHashMap<>();
        remediations.put("fix-prior", List.of(
            new FileChangeSpec("Shared.java", 2, 3, 1, 1, "s1\ns2\ns3\ns4", "s2\ns3", "P2\nP3")));
        remediations.put("fix-mixed", List.of(
            new FileChangeSpec("Example.java", 2, 2, 1, 1, "before\nTARGET\nafter", "TARGET", "REPLACED"),
            new FileChangeSpec("Shared.java", 3, 4, 1, 1, "s2\ns3\ns4\ns5", "s3\ns4", "M3\nM4")));
        Path fprPath = createRemediationFprWithRemediations(remediations);

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Conflicts with another fix", 1), metric.skippedByReason());
        assertEquals("s1\nP2\nP3\ns4\ns5\n", Files.readString(sharedFile));
        assertEquals("before\nTARGET\nafter\n", Files.readString(exampleFile));
    }

    /**
     * Near-identical recognition (Phase 2): two remediations at the same location whose NewCode
     * differs only by a trailing Java comment normalize to the same comparisonCode. The second
     * must be recognized as fully identical to the first and counted once, not re-applied and not
     * leaving any trace of its own NewCode text in the file.
     */
    @Test
    void fullyIdenticalNearIdenticalRemediationIsCountedOnceNotReapplied() throws Exception {
        Path sourceFile = writeSourceFile("before\nTARGET\nafter\n");
        Path fprPath = createRemediationFpr(List.of(
            new RemediationSpec("fix-1", 2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED"),
            new RemediationSpec("fix-2", 2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED // note")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(1, metric.identicalRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * All-or-nothing multi-file remediations: one remediation touching two files where one
     * file's hunk cannot be located must skip the WHOLE remediation, not just that file. Neither
     * file may end up written - a half-applied remediation is worse than one left unapplied.
     */
    @Test
    void multiFileRemediationSkipsEntirelyWhenAnyFileFails() throws Exception {
        Path badFile = writeSourceFile("Bad.java", "one\ntwo\nthree\n");
        Path goodFile = writeSourceFile("Good.java", "before\nTARGET\nafter\n");
        Path fprPath = createMultiFileRemediationFpr("multi-file-fix", List.of(
            new FileChangeSpec("Bad.java", 2, 2, 1, 1, "nomatch-a\nnomatch-b\nnomatch-c", "NOMATCH", "X"),
            new FileChangeSpec("Good.java", 2, 2, 1, 1, "before\nTARGET\nafter", "TARGET", "REPLACED")));

        RemediationMetric metric;
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            metric = new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }

        assertEquals(1, metric.totalRemediations());
        assertEquals(0, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Source context not found", 1), metric.skippedByReason());
        assertEquals(Set.of(), metric.modifiedFiles());
        assertEquals("before\nTARGET\nafter\n", Files.readString(goodFile));
        assertEquals("one\ntwo\nthree\n", Files.readString(badFile));
    }

    // ------------------------------------------------------------------------------------------
    // Folded in from the former RemediationProcessorKnownIssuesTest (REVIEW_2 §2.1 - §2.5) once
    // every scenario there turned green. Uses its own multi-hunk/multi-file harness below since
    // the builders above only support one hunk per file.
    // ------------------------------------------------------------------------------------------

    /**
     * A malformed remediation entry (missing {@code <r:Filename>}) is skipped on its own via
     * {@code REMEDIATION_DATA_INVALID} rather than aborting the whole batch; the well-formed
     * remediation alongside it still applies.
     */
    @Test
    void malformedRemediationIsSkippedAndValidRemediationsStillApply() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("malformed", List.of(new FileSpec(null, List.of(
                new HunkSpec(1, 1, 0, 0, "before", "before", "BROKEN"))))),
            new MultiHunkRemediationSpec("valid", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations(), "the valid remediation must still be applied");
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Remediation data invalid", 1), metric.skippedByReason());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * Hunks staged earlier in the same remediation must be visible to the offset projection of a
     * later hunk in the same file: hunk 1 replaces lines 2-3 with a single line (delta -1) and
     * hunk 2 targets the second of two identical {@code before/TARGET/after} blocks. With the
     * staged delta visible, the projected start lands exactly on one of the two context
     * candidates instead of being ambiguous.
     *
     * <p>Note {@link #offsetLedgerAccountsForDedupWhenProjectingLaterHunkInSameFile} looks similar
     * but does not cover this - it uses two separate remediations, so the first is committed
     * before the second is classified.
     */
    @Test
    void secondHunkOfSameRemediationProjectsThroughFirstHunksLineDelta() throws Exception {
        Path sourceFile = writeSourceFile("Example.java",
            "line1\nline2\nline3\nbefore\nTARGET\nafter\nbefore\nTARGET\nafter\ntail\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("two-hunks", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 3, 1, 1, "line1\nline2\nline3\nbefore", "line2\nline3", "X"),
                new HunkSpec(8, 8, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("line1\nX\nbefore\nTARGET\nafter\nbefore\nREPLACED\nafter\ntail\n",
            Files.readString(sourceFile));
    }

    /**
     * A remediation mixing an already-covered hunk and a still-applicable hunk must apply the
     * applicable one rather than being rejected wholesale: {@code wide-fix} covers lines 1-3,
     * {@code mixed-fix}'s first hunk (line 2) is covered by it, but its second hunk (line 5) is
     * untouched and must still land.
     */
    @Test
    void remediationMixingCoveredAndApplicableHunksStillAppliesTheApplicableOne() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\nkeep\nOTHER\ntail\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "before\nTARGET\nafter", "before\nTARGET\nafter", "W1\nW2\nW3"))))),
            new MultiHunkRemediationSpec("mixed-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "NARROW"),
                new HunkSpec(5, 5, 1, 1, "keep\nOTHER\ntail", "OTHER", "FIXED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("W1\nW2\nW3\nkeep\nFIXED\ntail\n", Files.readString(sourceFile));
    }

    /**
     * A remediation with any conflicting hunk must not be applied at all, even if its other
     * hunks would individually be POSSIBLY_REMEDIATED: {@code mixed-fix}'s hunk 1 (line 2) is
     * POSSIBLY_REMEDIATED against {@code wide-a}, and hunk 2 (lines 7-8) CONFLICTS with
     * {@code wide-b}'s declared range. The whole remediation must be skipped as conflicting,
     * not partially applied.
     */
    @Test
    void remediationWithAnyConflictingHunkIsSkippedAsConflicting() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "l1\nl2\nl3\nl4\nl5\nl6\nl7\nl8\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("wide-a", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "l1\nl2\nl3", "l1\nl2\nl3", "A1\nl2\nA3"))))),
            new MultiHunkRemediationSpec("wide-b", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(5, 7, 1, 1, "l4\nl5\nl6\nl7\nl8", "l5\nl6\nl7", "B5\nB6\nl7"))))),
            new MultiHunkRemediationSpec("mixed-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "l1\nl2\nl3", "l2", "M2"),
                new HunkSpec(7, 8, 1, 1, "l6\nl7\nl8", "l7\nl8", "M7\nM8")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(3, metric.totalRemediations());
        assertEquals(2, metric.appliedRemediations());
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Conflicts with another fix", 1), metric.skippedByReason());
        assertEquals("A1\nl2\nA3\nl4\nB5\nB6\nl7\nl8\n", Files.readString(sourceFile),
            "the conflicting hunk must not overwrite wide-b's region");
    }

    /**
     * NewCode line endings must follow the file's own separator rather than an unrelated
     * comment-marker-stripping side effect: for an extension unmapped by
     * {@code FileTypeLanguageMapperUtil}, a CRLF NewCode (the CR here is a literal
     * {@code &#13;} character reference) must not leave a stray CR in an LF-only source file.
     */
    @Test
    void newCodeWithCrlfDoesNotLeaveStrayCarriageReturnsInLfSourceFile() throws Exception {
        Path sourceFile = writeSourceFile("payload.zzz", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("crlf-newcode", List.of(new FileSpec("payload.zzz", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "NEW1&#13;\nNEW2")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(1, metric.appliedRemediations());
        assertEquals("before\nNEW1\nNEW2\nafter\n", Files.readString(sourceFile),
            "NewCode line endings must follow the file, not the XML payload");
    }

    /**
     * {@code contentCovers} must not treat an incidental substring hit as proof of coverage: the
     * broader fix's replacement happens to contain {@code return;} inside a null guard, while the
     * narrower fix proposes {@code return;} at a different line - they are not the same fix, so
     * the narrower remediation must classify as POSSIBLY_REMEDIATED, not SUPERSEDED.
     */
    @Test
    void incidentalSubstringMatchIsNotTreatedAsProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nif (value == null) { return; }\nprocess(value);"))))),
            new MultiHunkRemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "value = getInput();\nprocess(value);\nfinish();",
                    "process(value);", "return;")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.supersededRemediations(),
            "an incidental substring hit is not proof that the broader fix covers this hunk");
        assertEquals(1, metric.possiblyRemediatedRemediations());
        assertEquals("value = sanitize(getInput());\nif (value == null) { return; }\nprocess(value);\n",
            Files.readString(sourceFile), "only the broader fix may be written");
    }

    /**
     * {@code contentCovers} must not treat a blank candidate comparison code as proof of
     * coverage: a comment-only NewCode normalises to {@code ""}, and {@code anything.contains("")}
     * is trivially true, so this must not be misread as SUPERSEDED.
     */
    @Test
    void blankCandidateComparisonCodeIsNotTreatedAsProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "before\nTARGET\nafter", "before\nTARGET\nafter",
                    "W1\nW2\nW3"))))),
            // NewCode is a single line comment, so its comparison code normalises to "".
            new MultiHunkRemediationSpec("comment-only-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET",
                    "// nothing to change here")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.supersededRemediations(),
            "an empty candidate comparison code is not proof of coverage");
        assertEquals(1, metric.possiblyRemediatedRemediations());
        assertEquals("W1\nW2\nW3\n", Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // Folded in from the former RemediationProcessorRemainingIssuesTest (REVIEW_3 points 1-4) once
    // every scenario there turned green.
    // ------------------------------------------------------------------------------------------

    /**
     * A filename that is not a legal path on this OS ({@code FileChange.resolve} throwing
     * {@code InvalidPathException}) must be translated to {@code REMEDIATION_DATA_INVALID} and
     * skipped on its own, rather than escaping the per-remediation loop and aborting the whole
     * batch; the well-formed remediation alongside it must still apply.
     *
     * <p>Windows only: {@code ':'} is illegal in a Windows path but legal on Linux, so the
     * scenario cannot be reproduced there (the only character a Unix path rejects is NUL, which
     * XML 1.0 cannot carry).
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void illegalFilenameIsSkippedAndValidRemediationsStillApply() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("illegal-filename", List.of(new FileSpec("src/bad:name.java", List.of(
                new HunkSpec(1, 1, 0, 0, "before", "before", "BROKEN"))))),
            new MultiHunkRemediationSpec("valid", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations(), "the valid remediation must still be applied");
        assertEquals(1, metric.skippedRemediations());
        assertEquals(1, metric.skippedByReason().values().stream().mapToInt(Integer::intValue).sum(),
            "the illegal filename must be recorded as exactly one skip");
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    /**
     * A source file whose write fails (e.g., made read-only) must be skipped via
     * {@code SOURCE_WRITE_FAILED} on its own, not abort the whole batch: the rollback attempt for
     * that failed write must not itself retry writing to the same permission-denied file, which
     * previously escalated into a batch-aborting {@code RollbackRemediationException} and caused
     * every other remediation in the run - even ones touching unrelated, writable files - to fail.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void readOnlyFileWriteFailureIsSkippedAndOtherRemediationsStillApply() throws Exception {
        Path readOnlyFile = writeSourceFile("ReadOnly.java", "before\nTARGET\nafter\n");
        Path goodFile = writeSourceFile("Good.java", "before\nTARGET\nafter\n");
        readOnlyFile.toFile().setReadOnly();
        try {
            Path fprPath = buildFpr(List.of(
                new MultiHunkRemediationSpec("blocked", List.of(new FileSpec("ReadOnly.java", List.of(
                    new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED"))))),
                new MultiHunkRemediationSpec("valid", List.of(new FileSpec("Good.java", List.of(
                    new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

            RemediationMetric metric = apply(fprPath);

            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations(), "the remediation for the writable file must still be applied");
            assertEquals(1, metric.skippedRemediations());
            assertEquals(Map.of("Source file write failed", 1), metric.skippedByReason());
            assertEquals("before\nTARGET\nafter\n", Files.readString(readOnlyFile));
            assertEquals("before\nREPLACED\nafter\n", Files.readString(goodFile));
        } finally {
            readOnlyFile.toFile().setWritable(true);
        }
    }

    /**
     * The offset ledger must record where a hunk actually landed via the fuzzy anchor, not where
     * it was declared: {@code relocated} declares line 12 but its context/OriginalCode only exist
     * at line 3, so it lands there with delta +2 and the ledger must stage {@code (3, 3, +2)} -
     * not {@code (12, 12, +2)}. {@code later} genuinely targets the second {@code DUP} block at
     * declared line 11, which needs that correctly-staged shift to disambiguate between the two
     * identical context blocks; a stale staged range causes the shift to compute as 0 and the
     * exact-position disambiguation to match neither candidate.
     */
    @Test
    void offsetLedgerRecordsWhereHunkActuallyLandedNotWhereItWasDeclared() throws Exception {
        Path sourceFile = writeSourceFile("Example.java",
            "aa\nHEAD\nTARGET\nTAIL\nbb\nctx\nDUP\nctx2\ncc\nctx\nDUP\nctx2\ndd\n");
        Path fprPath = buildFpr(List.of(
            // Declared line 12 is stale; the anchor really sits at line 3.
            new MultiHunkRemediationSpec("relocated", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(12, 12, 1, 1, "HEAD\nTARGET\nTAIL", "TARGET", "R1\nR2\nR3"))))),
            // Genuinely targets the SECOND DUP block, declared (correctly) at line 11.
            new MultiHunkRemediationSpec("later", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(11, 11, 1, 1, "ctx\nDUP\nctx2", "DUP", "FIXED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("aa\nHEAD\nR1\nR2\nR3\nTAIL\nbb\nctx\nDUP\nctx2\ncc\nctx\nFIXED\nctx2\ndd\n",
            Files.readString(sourceFile));
    }

    /**
     * {@code contentCovers} must not treat an incidental substring hit as proof of coverage even
     * above the minimum-length guard: {@code return null;} normalises to 11 characters and occurs
     * in the wide fix's replacement only incidentally, inside a null guard several lines away from
     * where the narrow fix targets - they are not the same fix, so this must classify as
     * POSSIBLY_REMEDIATED, not SUPERSEDED.
     */
    @Test
    void incidentalSubstringAboveTheLengthGuardIsNotProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path fprPath = buildFpr(List.of(
            new MultiHunkRemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nif (value == null) { return null; }\nprocess(value);"))))),
            new MultiHunkRemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "value = getInput();\nprocess(value);\nfinish();",
                    "process(value);", "return null;")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.supersededRemediations(),
            "an incidental substring hit is not proof that the broader fix covers this hunk, "
                + "however long the candidate happens to be");
        assertEquals(1, metric.possiblyRemediatedRemediations());
        assertEquals("value = sanitize(getInput());\nif (value == null) { return null; }\nprocess(value);\n",
            Files.readString(sourceFile), "only the broader fix may be written");
    }

    /**
     * Safety property that per-FPR ledgers depend on: {@code apply-remediations --all-open-issues}
     * uses a fresh {@code RemediationProcessor} (and therefore a fresh ledger) per artifact, since
     * different artifacts may be scans of different source revisions. What keeps a multi-FPR run
     * safe is not the ledger but anchor verification - once an earlier FPR has rewritten a file,
     * the declared hash no longer matches, so a later FPR's hunk goes through the projection/fuzzy
     * path and is written only where its OriginalCode still literally matches. Here the narrow
     * fix's OriginalCode was consumed by the wide fix from the first FPR, so the second FPR must
     * skip it rather than write it against stale text.
     */
    @Test
    void secondFprDoesNotApplyOverAFixTheFirstFprAlreadyRewrote() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path firstFpr = buildFpr("artifact-1.fpr", List.of(
            new MultiHunkRemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nprocess(sanitized);\nfinish();")))))));
        Path secondFpr = buildFpr("artifact-2.fpr", List.of(
            new MultiHunkRemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "value = getInput();\nprocess(value);\nfinish();",
                    "process(value);", "process(escape(value));")))))));

        RemediationMetric first = apply(firstFpr);
        assertEquals(1, first.appliedRemediations());
        String afterFirst = "value = sanitize(getInput());\nprocess(sanitized);\nfinish();\n";
        assertEquals(afterFirst, Files.readString(sourceFile));

        RemediationMetric second = apply(secondFpr);

        assertEquals(0, second.appliedRemediations(),
            "the narrower fix's OriginalCode no longer exists, so it must not be written anywhere");
        assertEquals(1, second.skippedRemediations());
        assertEquals(afterFirst, Files.readString(sourceFile), "the second FPR must leave the file untouched");
    }

    private record HunkSpec(int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) {}

    /** A {@code <r:FileChanges>} block; a {@code null} filename omits the element entirely. */
    private record FileSpec(String filename, List<HunkSpec> hunks) {}

    private record MultiHunkRemediationSpec(String instanceId, List<FileSpec> files) {}

    private RemediationMetric apply(Path fprPath) throws Exception {
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            return new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }
    }

    private Path buildFpr(List<MultiHunkRemediationSpec> specs) throws Exception {
        return buildFpr("remediation.fpr", specs);
    }

    private Path buildFpr(String fprName, List<MultiHunkRemediationSpec> specs) throws Exception {
        StringBuilder remediations = new StringBuilder();
        for (MultiHunkRemediationSpec spec : specs) {
            remediations.append("<r:Remediation instanceId=\"").append(spec.instanceId()).append("\">\n")
                .append("  <r:AuditComment>test</r:AuditComment>\n");
            for (FileSpec file : spec.files()) {
                remediations.append("  <r:FileChanges>\n");
                if (file.filename() != null) {
                    remediations.append("    <r:Filename>").append(file.filename()).append("</r:Filename>\n");
                }
                remediations.append("    <r:Hash type=\"SHA-256\">not-the-source-hash</r:Hash>\n");
                for (HunkSpec hunk : file.hunks()) {
                    remediations.append("""
                                <r:Change>
                                  <r:LineFrom>%d</r:LineFrom>
                                  <r:LineTo>%d</r:LineTo>
                                  <r:Context before="%d" after="%d">%s</r:Context>
                                  <r:OriginalCode>%s</r:OriginalCode>
                                  <r:NewCode>%s</r:NewCode>
                                </r:Change>
                            """.formatted(hunk.lineFrom(), hunk.lineTo(), hunk.contextBefore(),
                            hunk.contextAfter(), hunk.context(), hunk.originalCode(), hunk.newCode()));
                }
                remediations.append("  </r:FileChanges>\n");
            }
            remediations.append("</r:Remediation>\n");
        }
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  %s
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, remediations);

        Path fprPath = tempDir.resolve(fprName);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String entryName, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String remediationsXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n        oldTwo();</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId=\"ISSUE-2\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>4</LineFrom>
                                <LineTo>4</LineTo>
                                <Context>        oldOne();\n        oldTwo();\n    }</Context>
                                <OriginalCode>        oldTwo();</OriginalCode>
                                <NewCode>        newTwo();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash, hash);
    }

    private String singleRemediationXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash);
    }

    private String pathTraversalAndValidRemediationsXml(String hash) {
        return """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <Remediations xmlns=\"xmlns://www.fortify.com/schema/remediations\">
                    <Remediation instanceId=\"ISSUE-TRAVERSAL\">
                        <FileChanges>
                            <Filename>../outside.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        ignored();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId=\"ISSUE-1\">
                        <FileChanges>
                            <Filename>Example.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>3</LineFrom>
                                <LineTo>3</LineTo>
                                <Context>    void run() {\n        oldOne();\n    }</Context>
                                <OriginalCode>        oldOne();</OriginalCode>
                                <NewCode>        newOne();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(hash, hash);
    }

    @Test
    void previewModeDoesNotModifySourceFiles() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-unchanged"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "        oldTwo();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(remediationsXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                options(Set.of(), RemediationExecutionMode.PREVIEW));
            var metric = processor.processRemediationXML();

            assertEquals(2, metric.totalRemediations());
            assertEquals(2, metric.appliedRemediations());
            assertTrue(metric.isPreview());

            String actualContent = Files.readString(sourceFile, StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(originalContent, actualContent);
            assertTrue(actualContent.contains("        oldOne();"));
            assertTrue(actualContent.contains("        oldTwo();"));
            assertFalse(actualContent.contains("newOne"));
            assertFalse(actualContent.contains("newTwo"));
        }
    }

    @Test
    void publicPreviewAdapterDoesNotModifySourceFiles() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-adapter"));
        Path sourceFile = sourceDir.resolve("Example.java");
        Files.writeString(sourceFile, "before\nTARGET\nafter\n", StandardCharsets.UTF_8);
        String originalContent = Files.readString(sourceFile, StandardCharsets.UTF_8);
        Path fprPath = createRemediationFpr(2, 2, 1, 1, "before\nTARGET\nafter", "TARGET", "REPLACED");

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            RemediationMetric metric = ApplyAutoRemediationOnSource.applyRemediations(
                fprHandle, sourceDir.toString(), null, Set.of(), true);

            assertTrue(metric.isPreview());
            assertEquals(1, metric.appliedRemediations());
        }

        assertEquals(originalContent, Files.readString(sourceFile, StandardCharsets.UTF_8));
    }

    @Test
    void previewModePopulatesPreviewDetailsWithChanges() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-details"));
        Path sourceFile = sourceDir.resolve("Example.java");
        String originalContent = String.join("\n",
                "class Example {",
                "    void run() {",
                "        oldOne();",
                "    }",
                "}",
                "");
        Files.writeString(sourceFile, originalContent, StandardCharsets.UTF_8);

        String hash = TestHashUtil.sha256Base64Unix(originalContent);
        Path fprPath = createFpr(singleRemediationXml(hash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                options(Set.of(), RemediationExecutionMode.PREVIEW));
            var metric = processor.processRemediationXML();

            assertTrue(metric.isPreview());
            assertEquals(1, metric.previewDetails().size());

            var detail = metric.previewDetails().get(0);
            assertEquals("ISSUE-1", detail.issueId());
            assertEquals("available", detail.status());
            assertNotNull(detail.files());
            assertEquals(1, detail.files().size());
            
            var filePreview = detail.files().get("Example.java");
            assertNotNull(filePreview);
            assertEquals("UTF-8", filePreview.encoding());
            assertEquals(1, filePreview.changes().size());
            
            var change = filePreview.changes().get(0);
            assertEquals(1, change.changeIndex());
            assertEquals(3, change.lineFrom());
            assertEquals(3, change.lineTo());
            assertTrue(change.originalCode().contains("oldOne"));
            assertTrue(change.newCode().contains("newOne"));
            assertFalse(change.fuzzyMatched());
        }
    }

    @Test
    void previewShowsDeclaredXmlFieldsWhenSourceHashDoesNotMatch() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-xml-only"));
        Path sourceFile = sourceDir.resolve("Example.java");
        Files.writeString(sourceFile, "class Example {\n    void run() {\n        drifted();\n    }\n}\n",
            StandardCharsets.UTF_8);

        String xmlHash = TestHashUtil.sha256Base64Unix("class Example {\n    void run() {\n        oldOne();\n    }\n}\n");
        Path fprPath = createFpr(singleRemediationXml(xmlHash));

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var metric = new RemediationProcessor(fprHandle, sourceDir.toString(),
                options(Set.of(), RemediationExecutionMode.PREVIEW)).processRemediationXML();

            assertTrue(metric.isPreview());
            assertEquals(1, metric.appliedRemediations());
            var change = metric.previewDetails().get(0).files().get("Example.java").changes().get(0);
            assertEquals(3, change.lineFrom());
            assertEquals(3, change.lineTo());
            assertTrue(change.originalCode().contains("oldOne"));
            assertTrue(change.newCode().contains("newOne"));
            assertFalse(change.fuzzyMatched());
        }
        assertTrue(Files.readString(sourceFile, StandardCharsets.UTF_8).contains("drifted();"));
    }

    @Test
    void previewModeCapturesSkipReasonsInPreviewDetails() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-skip"));
        // Create file for one remediation, but not the other
        Path validFile = sourceDir.resolve("Valid.java");
        Files.writeString(validFile, "class Valid { void run() { old(); } }", StandardCharsets.UTF_8);

        String validHash = TestHashUtil.sha256Base64Unix("class Valid { void run() { old(); } }");
        String missingHash = "dGVzdGhhc2g="; // arbitrary hash for missing file
        
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
                    <Remediation instanceId="ISSUE-VALID">
                        <FileChanges>
                            <Filename>Valid.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>1</LineFrom>
                                <LineTo>1</LineTo>
                                <Context>class Valid { void run() { old(); } }</Context>
                                <OriginalCode>old();</OriginalCode>
                                <NewCode>new();</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                    <Remediation instanceId="ISSUE-MISSING">
                        <FileChanges>
                            <Filename>Missing.java</Filename>
                            <Hash>%s</Hash>
                            <Change>
                                <LineFrom>1</LineFrom>
                                <LineTo>1</LineTo>
                                <Context>ignored</Context>
                                <OriginalCode>ignored</OriginalCode>
                                <NewCode>ignored</NewCode>
                            </Change>
                        </FileChanges>
                    </Remediation>
                </Remediations>
                """.formatted(validHash, missingHash);

        Path fprPath = tempDir.resolve("test-skip.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Valid.java</Name>
                          </File>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Missing.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """);
            writeEntry(zipOutputStream, "remediations.xml", xml);
        }

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                options(Set.of(), RemediationExecutionMode.PREVIEW));
            var metric = processor.processRemediationXML();

            assertEquals(2, metric.totalRemediations());
            assertEquals(1, metric.appliedRemediations());
            assertEquals(1, metric.skippedRemediations());

            assertTrue(metric.isPreview());
            assertEquals(2, metric.previewDetails().size());

            var available = metric.previewDetails().stream()
                    .filter(d -> "available".equals(d.status()))
                    .findFirst().orElseThrow();
            assertEquals("ISSUE-VALID", available.issueId());
            assertNotNull(available.files().get("Valid.java"));
            
            var skipped = metric.previewDetails().stream()
                    .filter(d -> "skipped".equals(d.status()))
                    .findFirst().orElseThrow();
            assertEquals("ISSUE-MISSING", skipped.issueId());
            assertEquals("Source file missing", skipped.skipReason());
            assertTrue(skipped.files().isEmpty());
        }
    }

    @Test
    void previewModeWithEmptyRemediationsXmlReturnsEmptyList() throws Exception {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-preview-empty"));
        Path sourceFile = sourceDir.resolve("Example.java");
        Files.writeString(sourceFile, "class Example { }", StandardCharsets.UTF_8);

        String emptyXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Remediations xmlns="xmlns://www.fortify.com/schema/remediations">
                </Remediations>
                """;

        Path fprPath = tempDir.resolve("test-empty.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zipOutputStream, "audit.fvdl", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <FVDL>
                      <Build>
                        <SourceFiles>
                          <File type="JAVA" encoding="UTF-8">
                            <Name>Example.java</Name>
                          </File>
                        </SourceFiles>
                      </Build>
                    </FVDL>
                    """);
            writeEntry(zipOutputStream, "remediations.xml", emptyXml);
        }

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            var processor = new RemediationProcessor(fprHandle, sourceDir.toString(),
                    options(Set.of(), RemediationExecutionMode.PREVIEW));
            var metric = processor.processRemediationXML();

            assertEquals(0, metric.totalRemediations());
            assertEquals(0, metric.appliedRemediations());

            assertTrue(metric.isPreview());
            assertEquals(List.of(), metric.previewDetails());
        }
    }

    private RemediationProcessingOptions options(Set<String> issueIds, RemediationExecutionMode executionMode) {
        return new RemediationProcessingOptions(issueIds, executionMode, SourceDecoders.defaults());
    }

    private static final class TestHashUtil {
        private static String sha256Base64Unix(String content) {
            try {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] digest = md.digest(content.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
                return java.util.Base64.getEncoder().encodeToString(digest);
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static String sha256Base64(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(bytes));
    }

    private Path writeSourceFile(String content) throws Exception {
        return writeSourceFile("Example.java", content);
    }

    private Path writeSourceFile(String filename, String content) throws Exception {
        Path sourceFile = tempDir.resolve(filename);
        Files.writeString(sourceFile, content, StandardCharsets.UTF_8);
        return sourceFile;
    }

    private record RemediationSpec(String instanceId, int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) {}

    private Path createRemediationFpr(List<RemediationSpec> specs) throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        StringBuilder remediations = new StringBuilder();
        for (RemediationSpec spec : specs) {
            remediations.append("""
                    <r:Remediation instanceId="%s">
                        <r:AuditComment>test</r:AuditComment>
                        <r:FileChanges>
                          <r:Filename>Example.java</r:Filename>
                          <r:Hash type="SHA-256">not-the-source-hash</r:Hash>
                          <r:Change>
                            <r:LineFrom>%d</r:LineFrom>
                            <r:LineTo>%d</r:LineTo>
                            <r:Context before="%d" after="%d">%s</r:Context>
                            <r:OriginalCode>%s</r:OriginalCode>
                            <r:NewCode>%s</r:NewCode>
                          </r:Change>
                        </r:FileChanges>
                      </r:Remediation>
                    """.formatted(spec.instanceId(), spec.lineFrom(), spec.lineTo(), spec.contextBefore(),
                    spec.contextAfter(), spec.context(), spec.originalCode(), spec.newCode()));
        }
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  %s
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, remediations);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    private Path createRemediationFpr(int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  <r:Remediation instanceId="issue-1">
                    <r:AuditComment>test</r:AuditComment>
                    <r:FileChanges>
                      <r:Filename>Example.java</r:Filename>
                      <r:Hash type="SHA-256">not-the-source-hash</r:Hash>
                      <r:Change>
                        <r:LineFrom>%d</r:LineFrom>
                        <r:LineTo>%d</r:LineTo>
                        <r:Context before="%d" after="%d">%s</r:Context>
                        <r:OriginalCode>%s</r:OriginalCode>
                        <r:NewCode>%s</r:NewCode>
                      </r:Change>
                    </r:FileChanges>
                  </r:Remediation>
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, lineFrom, lineTo, contextBefore, contextAfter,
                context, originalCode, newCode);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    private Path createRemediationFprWithHash(int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode, String hash) throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  <r:Remediation instanceId="issue-1">
                    <r:AuditComment>test</r:AuditComment>
                    <r:FileChanges>
                      <r:Filename>Example.java</r:Filename>
                      <r:Hash type="SHA-256">%s</r:Hash>
                      <r:Change>
                        <r:LineFrom>%d</r:LineFrom>
                        <r:LineTo>%d</r:LineTo>
                        <r:Context before="%d" after="%d">%s</r:Context>
                        <r:OriginalCode>%s</r:OriginalCode>
                        <r:NewCode>%s</r:NewCode>
                      </r:Change>
                    </r:FileChanges>
                  </r:Remediation>
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, hash, lineFrom, lineTo, contextBefore, contextAfter,
                context, originalCode, newCode);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    private record FileChangeSpec(String filename, int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) {}

    private Path createMultiFileRemediationFpr(String instanceId, List<FileChangeSpec> fileChangeSpecs) throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        StringBuilder fileChanges = new StringBuilder();
        for (FileChangeSpec spec : fileChangeSpecs) {
            fileChanges.append("""
                    <r:FileChanges>
                      <r:Filename>%s</r:Filename>
                      <r:Hash type="SHA-256">not-the-source-hash</r:Hash>
                      <r:Change>
                        <r:LineFrom>%d</r:LineFrom>
                        <r:LineTo>%d</r:LineTo>
                        <r:Context before="%d" after="%d">%s</r:Context>
                        <r:OriginalCode>%s</r:OriginalCode>
                        <r:NewCode>%s</r:NewCode>
                      </r:Change>
                    </r:FileChanges>
                    """.formatted(spec.filename(), spec.lineFrom(), spec.lineTo(), spec.contextBefore(),
                    spec.contextAfter(), spec.context(), spec.originalCode(), spec.newCode()));
        }
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  <r:Remediation instanceId="%s">
                    <r:AuditComment>test</r:AuditComment>
                    %s
                  </r:Remediation>
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, instanceId, fileChanges);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }

    /**
     * Builds an FPR with multiple remediations (insertion order preserved), each of which may
     * itself contain multiple file changes/hunks - used to construct mixed-outcome scenarios
     * where hunks within the same remediation classify differently against the ledger.
     */
    private Path createRemediationFprWithRemediations(LinkedHashMap<String, List<FileChangeSpec>> remediationFileChanges)
            throws Exception {
        Path fprPath = tempDir.resolve("remediation.fpr");
        StringBuilder remediationsXml = new StringBuilder();
        for (Map.Entry<String, List<FileChangeSpec>> entry : remediationFileChanges.entrySet()) {
            StringBuilder fileChanges = new StringBuilder();
            for (FileChangeSpec spec : entry.getValue()) {
                fileChanges.append("""
                        <r:FileChanges>
                          <r:Filename>%s</r:Filename>
                          <r:Hash type="SHA-256">not-the-source-hash</r:Hash>
                          <r:Change>
                            <r:LineFrom>%d</r:LineFrom>
                            <r:LineTo>%d</r:LineTo>
                            <r:Context before="%d" after="%d">%s</r:Context>
                            <r:OriginalCode>%s</r:OriginalCode>
                            <r:NewCode>%s</r:NewCode>
                          </r:Change>
                        </r:FileChanges>
                        """.formatted(spec.filename(), spec.lineFrom(), spec.lineTo(), spec.contextBefore(),
                        spec.contextAfter(), spec.context(), spec.originalCode(), spec.newCode()));
            }
            remediationsXml.append("""
                    <r:Remediation instanceId="%s">
                      <r:AuditComment>test</r:AuditComment>
                      %s
                    </r:Remediation>
                    """.formatted(entry.getKey(), fileChanges));
        }
        String remediationXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <r:Remediations xmlns:r="%s">
                  <r:ProjectInfo>
                    <r:Name>test</r:Name>
                    <r:WriteDate>2026-08-26T00:00:00Z</r:WriteDate>
                  </r:ProjectInfo>
                  <r:RemediationList>
                  %s
                  </r:RemediationList>
                </r:Remediations>
                """.formatted(REMEDIATIONS_NAMESPACE, remediationsXml);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }
}
