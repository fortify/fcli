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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.fpr.remediation.RemediationProcessor;
import com.fortify.cli.aviator.fpr.remediation.model.*;
import com.fortify.cli.aviator.util.FileUtil;
import com.fortify.cli.aviator.util.FprHandle;

class RemediationProcessorTest {
    private static final String REMEDIATIONS_NAMESPACE = "xmlns://www.fortify.com/schema/remediations";

    @TempDir
    Path tempDir;

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

    private Path createRemediationFpr(String context, String originalCode, String newCode) throws Exception {
        return createRemediationFpr(2, 2, 1, 1, context, originalCode, newCode);
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
}
