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
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.fpr.remediation.RemediationProcessor;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Deliberately-failing tests for the logic defects found in the third review (REVIEW_3 points 1-4).
 * Points 1-3 assert the DESIRED behaviour, not the current behaviour, so they are red today and turn
 * green when the corresponding defect is fixed. The point-4 test is green today and is meant to stay
 * green: it locks in the anchor-verification property that per-FPR ledgers depend on. The other half
 * of point 4, the artifact ordering, is red and lives in
 * {@code AviatorSSCApplyRemediationsCommandOrderTest} in the fcli-aviator module, since the ordering
 * belongs to the command rather than to this package.
 *
 * <p>Same shape as {@link RemediationProcessorKnownIssuesTest}: the builder supports several hunks
 * per file, several files per remediation, and (new here) several FPRs against one source tree.
 *
 * <p>Fold these scenarios into {@code RemediationProcessorTest} and delete this class once points
 * 1-3 pass.
 */
class RemediationProcessorRemainingIssuesTest {
    private static final String REMEDIATIONS_NAMESPACE = "xmlns://www.fortify.com/schema/remediations";

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------------------------------
    // 1. A filename that is not a legal path must be skipped, not abort the whole run.
    // ------------------------------------------------------------------------------------------

    /**
     * REVIEW_2 point 1 added a try/catch around {@code createRemediationKeys}, but it catches only
     * {@code SkipRemediationException}. {@code Path.resolve} throws {@code InvalidPathException}
     * for a filename that is not legal on this OS, and that one escapes the per-remediation loop;
     * {@code processRemediationXML} rethrows it as
     * {@code AviatorTechnicalException("Unexpected error processing remediations.xml.")}, so one
     * bad entry again costs the customer every other remediation in the FPR.
     *
     * <p>The fix is to translate at the source rather than to widen the catch, exactly as
     * {@code RequiredFields.requireInt} already translates {@code NumberFormatException}: let
     * {@code FileChange.resolve} catch {@code InvalidPathException} and throw
     * {@code SkipRemediationException(REMEDIATION_DATA_INVALID)}. The existing catch then handles
     * it and no new catch site is needed - including in {@code HunkClassifier}, whose own catch is
     * also {@code SkipRemediationException}-only. Note that {@code RemediationKey.of} re-implements
     * {@code resolve} instead of calling it, so it has to call {@code fileChange.resolve(...)} for
     * the fix to take effect on the path this test exercises.
     *
     * <p>Which {@code SkipReason} ends up being used is the implementer's choice, so this test
     * asserts the counts and the resulting source file rather than pinning the label.
     *
     * <p>Windows only: {@code ':'} is illegal in a Windows path but perfectly legal on Linux, where
     * the scenario therefore cannot be reproduced at all (the only character a Unix path rejects is
     * NUL, which XML 1.0 cannot carry). The defect and its fix are platform-independent even though
     * this reproduction is not.
     *
     * <p>Currently: throws {@code AviatorTechnicalException}, caused by {@code InvalidPathException}.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void illegalFilenameIsSkippedAndValidRemediationsStillApply() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("illegal-filename", List.of(new FileSpec("src/bad:name.java", List.of(
                new HunkSpec(1, 1, 0, 0, "before", "before", "BROKEN"))))),
            new RemediationSpec("valid", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations(), "the valid remediation must still be applied");
        assertEquals(1, metric.skippedRemediations());
        assertEquals(1, metric.skippedByReason().values().stream().mapToInt(Integer::intValue).sum(),
            "the illegal filename must be recorded as exactly one skip");
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // 2. The offset ledger must record where a hunk actually landed, not where it was declared.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code FileWriteCoordinator.processFileChanges} stages
     * {@code new PendingAppliedChange(filePath, declaredLineFrom, declaredLineTo, delta, ...)},
     * but {@code RemediationApplier.applyChange} may have relocated the hunk via the fuzzy anchor -
     * which is exactly the situation this whole feature exists for. The ledger then holds a range
     * the file was never edited at, and both things that read it are wrong: {@code projectOffset}
     * attributes the delta to the wrong position, and {@code HunkClassifier} compares later hunks
     * against a region that was never touched.
     *
     * <p>Here {@code relocated} declares line 12 but its context and OriginalCode only exist at
     * line 3, so it correctly lands there with delta +2 - and the ledger records {@code (12, 12, +2)}.
     * {@code later} genuinely targets the second {@code DUP} block at declared line 11. Because
     * {@code originalLineTo() == 12} is not {@code < 11}, the shift computes as 0 instead of +2, the
     * projection is fed line 11 instead of 13, and the exact-position disambiguation in
     * {@code searchContext} then matches neither of the two candidates.
     *
     * <p>With the actual range {@code (3, 3, +2)} staged, the shift is +2, the projected position
     * holds the expected OriginalCode, and the hunk applies on the fast path without any fuzzy
     * search at all.
     *
     * <p>Currently: skipped with {@code "Source context matched multiple locations"}, the second
     * DUP block never becomes {@code FIXED}.
     */
    @Test
    void offsetLedgerRecordsWhereHunkActuallyLandedNotWhereItWasDeclared() throws Exception {
        Path sourceFile = writeSourceFile("Example.java",
            "aa\nHEAD\nTARGET\nTAIL\nbb\nctx\nDUP\nctx2\ncc\nctx\nDUP\nctx2\ndd\n");
        Path fprPath = buildFpr(List.of(
            // Declared line 12 is stale; the anchor really sits at line 3.
            new RemediationSpec("relocated", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(12, 12, 1, 1, "HEAD\nTARGET\nTAIL", "TARGET", "R1\nR2\nR3"))))),
            // Genuinely targets the SECOND DUP block, declared (correctly) at line 11.
            new RemediationSpec("later", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(11, 11, 1, 1, "ctx\nDUP\nctx2", "DUP", "FIXED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("aa\nHEAD\nR1\nR2\nR3\nTAIL\nbb\nctx\nDUP\nctx2\ncc\nctx\nFIXED\nctx2\ndd\n",
            Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // 3. SUPERSEDED still rests on a substring-anywhere test.
    // ------------------------------------------------------------------------------------------

    /**
     * The javadoc of {@code RemediationProcessorKnownIssuesTest} test 5a suggested that either a
     * minimum-length guard on the candidate code or anchoring the match at the hunk's own offset
     * within the broader fix "would do it". The minimum-length guard was implemented
     * ({@code AppliedChange.MIN_PROVEN_COVERAGE_LENGTH = 8}) and it does turn 5a and 5b green, but
     * the suggestion itself was wrong: a length threshold moves the bar rather than removing the
     * cause. {@code contentCovers} still asks whether the broader fix's comparison code contains
     * the narrower one ANYWHERE.
     *
     * <p>{@code return;} normalises to 7 characters and is now rejected. {@code return null;}
     * normalises to {@code returnnull;}, 11 characters, and sails through - even though it occurs
     * in the broader fix only incidentally, inside a null guard several lines away from the line
     * the narrower fix targets. These are not the same fix. Test 5a used the short form, which is
     * why it could not tell the two approaches apart; this one deliberately uses the long form so
     * that only the offset-anchored comparison can satisfy it.
     *
     * <p>The consequence is the worst of the three outcomes: SUPERSEDED means the remediation is
     * dropped silently, with nothing recorded in {@code skippedByReason} and nothing for the
     * customer to see. POSSIBLY_REMEDIATED is the bucket for "location covered, coverage not
     * proven" and is the right answer.
     *
     * <p>Currently: superseded=1, possiblyRemediated=0.
     */
    @Test
    void incidentalSubstringAboveTheLengthGuardIsNotProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nif (value == null) { return null; }\nprocess(value);"))))),
            new RemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
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

    // ------------------------------------------------------------------------------------------
    // 4. Multi-FPR runs: the safety property that per-FPR ledgers depend on.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code apply-remediations --all-open-issues} calls {@code processRemediationXML()} once per
     * artifact, each with a fresh {@code RemediationProcessor} and therefore a fresh
     * {@code AppliedChangeLedger} and {@code remediationLookup}. Sharing one ledger across artifacts
     * is NOT the fix: {@code AppliedChange} is expressed in pristine-file line numbers, and
     * different artifacts are scans of potentially different source revisions, so a shared ledger
     * would project deltas from one coordinate system onto another.
     *
     * <p>Per-FPR ledgers are therefore a deliberate decision, and what keeps a multi-FPR run safe is
     * not the ledger but anchor verification: once an earlier FPR has touched a file, the declared
     * hash no longer matches, so every later hunk goes through the projection/fuzzy path and is
     * written only where its OriginalCode still literally matches. This test asserts that property
     * directly, so that it cannot be weakened by accident.
     *
     * <p>Currently: passes. Keep it passing.
     */
    @Test
    void secondFprDoesNotApplyOverAFixTheFirstFprAlreadyRewrote() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path firstFpr = buildFpr("artifact-1.fpr", List.of(
            new RemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nprocess(sanitized);\nfinish();")))))));
        Path secondFpr = buildFpr("artifact-2.fpr", List.of(
            new RemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
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

    // ------------------------------------------------------------------------------------------
    // Harness
    // ------------------------------------------------------------------------------------------

    private record HunkSpec(int lineFrom, int lineTo, int contextBefore, int contextAfter,
            String context, String originalCode, String newCode) {}

    /** A {@code <r:FileChanges>} block; a {@code null} filename omits the element entirely. */
    private record FileSpec(String filename, List<HunkSpec> hunks) {}

    private record RemediationSpec(String instanceId, List<FileSpec> files) {}

    private RemediationMetric apply(Path fprPath) throws Exception {
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            return new RemediationProcessor(fprHandle, tempDir.toString()).processRemediationXML();
        }
    }

    private Path writeSourceFile(String filename, String content) throws Exception {
        Path sourceFile = tempDir.resolve(filename);
        Files.writeString(sourceFile, content, StandardCharsets.UTF_8);
        return sourceFile;
    }

    private Path buildFpr(List<RemediationSpec> specs) throws Exception {
        return buildFpr("remediation.fpr", specs);
    }

    private Path buildFpr(String fprName, List<RemediationSpec> specs) throws Exception {
        StringBuilder remediations = new StringBuilder();
        for (RemediationSpec spec : specs) {
            remediations.append("<r:Remediation instanceId=\"").append(spec.instanceId()).append("\">\n")
                .append("  <r:AuditComment>test</r:AuditComment>\n");
            for (FileSpec file : spec.files()) {
                remediations.append("  <r:FileChanges>\n");
                if (file.filename() != null) {
                    remediations.append("    <r:Filename>").append(file.filename()).append("</r:Filename>\n");
                }
                // A hash that cannot match: the source has drifted since the scan.
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
}
