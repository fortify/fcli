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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.fpr.remediation.RemediationProcessor;
import com.fortify.cli.aviator.fpr.remediation.model.AppliedChange;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Deliberately-failing tests for the known logic defects in the remediation apply pipeline
 * (REVIEW_2 §2.1 - §2.5; §2.3 and §2.5 have more than one test each).
 * Each test asserts the DESIRED behaviour, not the current behaviour, so each one is red today and
 * turns green when the corresponding defect is fixed. They are kept in their own class so that the
 * expected-red set is obvious and {@link RemediationProcessorTest} stays all-green.
 *
 * <p>Unlike {@link RemediationProcessorTest}, the builder here supports several hunks per file and
 * several files per remediation, which is what most of these scenarios need.
 *
 * <p>Delete this class once all five pass — the scenarios themselves should be folded into
 * {@code RemediationProcessorTest} at that point.
 */
class RemediationProcessorKnownIssuesTest {
    private static final String REMEDIATIONS_NAMESPACE = "xmlns://www.fortify.com/schema/remediations";

    @TempDir
    Path tempDir;

    // ------------------------------------------------------------------------------------------
    // 1. A malformed remediation entry must be skipped, not abort the whole run.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code RemediationProcessor.classifyAndApply} calls {@code createRemediationKeys} outside any
     * try/catch, so the {@code SkipRemediationException(REMEDIATION_DATA_INVALID)} raised by the
     * lazy {@code requiredFilename()}/{@code lineFrom()} accessors escapes the per-remediation loop
     * and is rethrown from {@code processRemediationXML} as an {@code AviatorTechnicalException}.
     * One bad entry in remediations.xml therefore costs the customer every other remediation in the
     * FPR.
     *
     * <p>Desired: the malformed entry is skipped with {@code REMEDIATION_DATA_INVALID} and the
     * valid entry still applies. Note this is also the only call path that can raise that reason,
     * so today {@code "Remediation data invalid"} can never appear in {@code skippedByReason}.
     *
     * <p>Currently: throws {@code AviatorTechnicalException: Unexpected error processing
     * remediations.xml}.
     */
    @Test
    void malformedRemediationIsSkippedAndValidRemediationsStillApply() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            // No <r:Filename> element at all.
            new RemediationSpec("malformed", List.of(new FileSpec(null, List.of(
                new HunkSpec(1, 1, 0, 0, "before", "before", "BROKEN"))))),
            new RemediationSpec("valid", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(1, metric.appliedRemediations(), "the valid remediation must still be applied");
        assertEquals(1, metric.skippedRemediations());
        assertEquals(Map.of("Remediation data invalid", 1), metric.skippedByReason());
        assertEquals("before\nREPLACED\nafter\n", Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // 2. Hunks staged earlier in the same remediation must be visible to the offset projection.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code AppliedChangeLedger.projectOffset} and {@code changesFor} only read committed state,
     * but {@code FileWriteCoordinator.processFileChanges} applies every hunk of a file in one pass
     * against progressively-updated content. So the second hunk of a remediation is applied to
     * content the first hunk already shifted, while {@code RemediationApplier} still sees
     * {@code priorApplied.isEmpty()} and {@code shift == 0} — the fast projection path is skipped
     * and, worse, the exact-declared-position disambiguation in the fuzzy fallback is fed a stale
     * line number.
     *
     * <p>Here hunk 1 replaces lines 2-3 with a single line (delta -1) and hunk 2 targets the second
     * of two identical {@code before/TARGET/after} blocks. With the staged delta visible,
     * {@code shift == -1}, the projected start is exactly one of the two context candidates, and
     * the hunk lands cleanly.
     *
     * <p>Note {@code RemediationProcessorTest.offsetLedgerAccountsForDedupWhenProjectingLaterHunkInSameFile}
     * looks like it covers this but does not — it uses two separate remediations, so the first is
     * committed before the second is classified.
     *
     * <p>Currently: skipped with {@code "Source context matched multiple locations"}, file unchanged.
     */
    @Test
    void secondHunkOfSameRemediationProjectsThroughFirstHunksLineDelta() throws Exception {
        Path sourceFile = writeSourceFile("Example.java",
            "line1\nline2\nline3\nbefore\nTARGET\nafter\nbefore\nTARGET\nafter\ntail\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("two-hunks", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 3, 1, 1, "line1\nline2\nline3\nbefore", "line2\nline3", "X"),
                new HunkSpec(8, 8, 1, 1, "before\ntarget\nafter", "TARGET", "REPLACED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(1, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals(Map.of(), metric.skippedByReason());
        assertEquals("line1\nX\nbefore\nTARGET\nafter\nbefore\nREPLACED\nafter\ntail\n",
            Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // 3a. A remediation mixing already-covered and still-applicable hunks must apply the rest.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code classifyAndApply} reduces the per-hunk {@code List<HunkOutcome>} to four "all X"
     * booleans and only acts when every hunk agrees; anything mixed falls through to
     * {@code processRemediation}, which re-attempts ALL hunks because {@code keysToApply} is only
     * narrowed by the separate identity check. So a remediation with one hunk already covered by a
     * broader prior fix and one perfectly applicable hunk elsewhere fails on the covered hunk's
     * missing anchor and is lost in its entirety.
     *
     * <p>Desired: feed the classification into {@code keysToApply} the way the identity check
     * already does, so the covered hunk is filtered out and the applicable one lands. (If the team
     * prefers strict atomicity here instead — skip the whole remediation — flip this test to assert
     * that, but do it deliberately: the identity path already partial-applies, so the two paths are
     * inconsistent today either way.)
     *
     * <p>Currently: skipped with {@code "Anchor does not match"}; line 5 never becomes {@code FIXED}.
     */
    @Test
    void remediationMixingCoveredAndApplicableHunksStillAppliesTheApplicableOne() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\nkeep\nOTHER\ntail\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "before\nTARGET\nafter", "before\nTARGET\nafter", "W1\nW2\nW3"))))),
            new RemediationSpec("mixed-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "NARROW"),
                new HunkSpec(5, 5, 1, 1, "keep\nOTHER\ntail", "OTHER", "FIXED")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(2, metric.totalRemediations());
        assertEquals(2, metric.appliedRemediations());
        assertEquals(0, metric.skippedRemediations());
        assertEquals("W1\nW2\nW3\nkeep\nFIXED\ntail\n", Files.readString(sourceFile));
    }

    // ------------------------------------------------------------------------------------------
    // 3b. A remediation with any conflicting hunk must not be applied at all.
    // ------------------------------------------------------------------------------------------

    /**
     * Same root cause as above, but this one writes to disk. {@code allPossiblyRemediated} requires
     * {@code noneMatch(CONFLICTS)} and {@code allConflicts} requires all, so a remediation whose
     * hunks are part covered-by-a-broader-fix and part conflicting matches none of the three guards
     * and is attempted anyway.
     *
     * <p>Setup: {@code wide-a} rewrites lines 1-3 but leaves line 2 textually unchanged (so
     * {@code mixed-fix}'s first hunk is still anchorable), and {@code wide-b} rewrites lines 5-7 but
     * leaves line 7 unchanged. {@code mixed-fix} hunk 1 (line 2) is POSSIBLY_REMEDIATED, hunk 2
     * (lines 7-8) CONFLICTS with {@code wide-b}'s declared range — and both anchors still resolve,
     * so {@code mixed-fix} is written and clobbers line 7 of {@code wide-b}'s region. That is
     * precisely what the CONFLICTS outcome exists to prevent.
     *
     * <p>Currently: applied=3, the file ends {@code ...B5/B6/M7/M8}.
     */
    @Test
    void remediationWithAnyConflictingHunkIsSkippedAsConflicting() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "l1\nl2\nl3\nl4\nl5\nl6\nl7\nl8\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("wide-a", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "l1\nl2\nl3", "l1\nl2\nl3", "A1\nl2\nA3"))))),
            new RemediationSpec("wide-b", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(5, 7, 1, 1, "l4\nl5\nl6\nl7\nl8", "l5\nl6\nl7", "B5\nB6\nl7"))))),
            new RemediationSpec("mixed-fix", List.of(new FileSpec("Example.java", List.of(
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

    // ------------------------------------------------------------------------------------------
    // 4. NewCode line endings must be normalised to the file's own separator.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code RemediationApplier.applyChange} splits NewCode on {@code "\n"} and relies on
     * {@code FileUtil.stripSyntheticLineMarkers} having normalised {@code \R} to {@code \n} as a
     * side effect. That side effect only happens when the file extension maps to a known comment
     * symbol; for an unmapped extension the helper returns the content untouched, so every line of
     * a CRLF NewCode keeps a trailing {@code \r} which is then rejoined with the file's own
     * separator — writing a stray CR into the source.
     *
     * <p>{@code applyChange} already has {@code normalizeLineEndings} for the file content; NewCode
     * should get the same treatment instead of depending on an unrelated helper.
     *
     * <p>This test deliberately uses an extension that {@code FileTypeLanguageMapperUtil} does not
     * map. The NewCode carries a literal CR via the {@code &#13;} character reference.
     *
     * <p>Currently: writes {@code before\nNEW1\r\nNEW2\nafter\n}.
     */
    @Test
    void newCodeWithCrlfDoesNotLeaveStrayCarriageReturnsInLfSourceFile() throws Exception {
        Path sourceFile = writeSourceFile("payload.zzz", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("crlf-newcode", List.of(new FileSpec("payload.zzz", List.of(
                new HunkSpec(2, 2, 1, 1, "before\ntarget\nafter", "TARGET", "NEW1&#13;\nNEW2")))))));

        RemediationMetric metric = apply(fprPath);

        assertEquals(1, metric.appliedRemediations());
        assertEquals("before\nNEW1\nNEW2\nafter\n", Files.readString(sourceFile),
            "NewCode line endings must follow the file, not the XML payload");
    }

    // ------------------------------------------------------------------------------------------
    // 5. SUPERSEDED must mean the broader fix's content covers this hunk, not merely contains it.
    //
    //    contentCovers has three ways of claiming coverage it hasn't proven, worst first:
    //      (a) incidental substring    -> reachable, short generic replacement
    //      (b) blank candidate         -> reachable, comment-only NewCode
    //      (c) either side null        -> unreachable end-to-end, unit-tested below
    //    All three report the remediation as SUPERSEDED, i.e. silently dropped with no skip reason
    //    recorded anywhere. POSSIBLY_REMEDIATED is the bucket that exists for "location covered,
    //    coverage not proven" and is the right answer for all three.
    // ------------------------------------------------------------------------------------------

    /**
     * 5a. {@code contentCovers} asks whether the broader fix's comparison code contains the
     * narrower fix's comparison code ANYWHERE, not whether it covers it at the narrower fix's
     * location. Short, generic replacements hit incidentally.
     *
     * <p>Here the broader fix's replacement happens to include {@code return;} inside a null guard;
     * the narrower fix proposes {@code return;} at a different line. They are not the same fix, but
     * the substring test says they are, so the narrower remediation is reported as SUPERSEDED.
     *
     * <p>Desired: POSSIBLY_REMEDIATED. A minimum-length guard on the candidate code, or anchoring
     * the match at the hunk's own offset within the broader fix, would do it.
     *
     * <p>Currently: superseded=1, possiblyRemediated=0.
     */
    @Test
    void incidentalSubstringMatchIsNotTreatedAsProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "value = getInput();\nprocess(value);\nfinish();\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0,
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = getInput();\nprocess(value);\nfinish();",
                    "value = sanitize(getInput());\nif (value == null) { return; }\nprocess(value);"))))),
            new RemediationSpec("narrow-fix", List.of(new FileSpec("Example.java", List.of(
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
     * 5b. {@code createComparisonCode} strips comments and then all whitespace, so a NewCode that
     * is nothing but a comment normalises to {@code ""} — and {@code anything.contains("")} is
     * always {@code true}. Every comment-only remediation therefore classifies as SUPERSEDED
     * against any broader fix covering its lines, regardless of content.
     *
     * <p>Desired: a blank candidate proves nothing, so POSSIBLY_REMEDIATED.
     *
     * <p>Currently: superseded=1, possiblyRemediated=0.
     */
    @Test
    void blankCandidateComparisonCodeIsNotTreatedAsProvenSupersession() throws Exception {
        Path sourceFile = writeSourceFile("Example.java", "before\nTARGET\nafter\n");
        Path fprPath = buildFpr(List.of(
            new RemediationSpec("wide-fix", List.of(new FileSpec("Example.java", List.of(
                new HunkSpec(1, 3, 0, 0, "before\nTARGET\nafter", "before\nTARGET\nafter",
                    "W1\nW2\nW3"))))),
            // NewCode is a single line comment, so its comparison code normalises to "".
            new RemediationSpec("comment-only-fix", List.of(new FileSpec("Example.java", List.of(
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

    /**
     * 5c. {@code AppliedChange.contentCovers} returns {@code true} when either side's comparison
     * code is {@code null}, and {@code HunkClassifier.classifyRange} turns that into SUPERSEDED —
     * its javadoc calls this "the conservative range-only default". It isn't conservative: the
     * consequence is that the remediation is dropped and not even counted as skipped. Unavailable
     * content is the textbook case for POSSIBLY_REMEDIATED, so {@code contentCovers} should say
     * {@code false}.
     *
     * <p>This one is asserted directly on {@link AppliedChange} rather than end-to-end, because the
     * {@code null} branches are currently unreachable through {@code processRemediationXML}:
     * {@code Hunk.comparisonCode} delegates to {@code requiredNewCode()}, which throws rather than
     * returning {@code null}, and both normalisation steps only return {@code null} for
     * {@code null} input. And {@code HunkClassifier}'s {@code catch} that sets the candidate to
     * {@code null} can't fire either, because {@code createRemediationKeys} computes the very same
     * comparison code first and blows up the whole run (see §2.1) before the classifier is reached.
     *
     * <p>So this is really a wrong default in dead defensive code rather than a live defect — worth
     * fixing so it can't come alive later, but rank it below 5a/5b.
     *
     * <p>Currently: both assertions return {@code true}.
     */
    @Test
    void unavailableComparisonCodeIsNotTreatedAsProvenCoverage() {
        assertFalse(new AppliedChange("wide-fix", 1, 3, 0, "W1W2W3").contentCovers(null),
            "a candidate whose content could not be computed has not been proven covered");
        assertFalse(new AppliedChange("wide-fix", 1, 3, 0, null).contentCovers("M2"),
            "an applied change whose content is unknown cannot prove it covers anything");
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
        StringBuilder remediations = new StringBuilder();
        for (RemediationSpec spec : specs) {
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

        Path fprPath = tempDir.resolve("remediation.fpr");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            zipOutputStream.putNextEntry(new ZipEntry("remediations.xml"));
            zipOutputStream.write(remediationXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return fprPath;
    }
}
