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
package com.fortify.cli.aviator.fpr.remediation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.fpr.remediation.applier.RemediationApplier;
import com.fortify.cli.aviator.fpr.remediation.classifier.AppliedChangeLedger;
import com.fortify.cli.aviator.fpr.remediation.classifier.HunkClassifier;
import com.fortify.cli.aviator.fpr.remediation.exception.RemediationCommitException;
import com.fortify.cli.aviator.fpr.remediation.exception.RollbackRemediationException;
import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;
import com.fortify.cli.aviator.fpr.remediation.model.FileChange;
import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.fpr.remediation.model.HunkOutcome;
import com.fortify.cli.aviator.fpr.remediation.model.Remediation;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationDocument;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationKey;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.remediation.preview.ChangeDetail;
import com.fortify.cli.aviator.fpr.remediation.preview.FilePreview;
import com.fortify.cli.aviator.fpr.remediation.preview.PreviewFileChange;
import com.fortify.cli.aviator.fpr.remediation.writer.FileWriteCoordinator;
import com.fortify.cli.aviator.fpr.remediation.writer.PendingFileWrite;
import com.fortify.cli.aviator.fpr.remediation.writer.PreparedFileChanges;
import com.fortify.cli.aviator.fpr.remediation.xmlprocessor.RemediationDocumentMapper;
import com.fortify.cli.aviator.fpr.remediation.xmlprocessor.RemediationXmlReader;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Orchestrator. {@link #processRemediationXML()} runs the three phases in sequence: parse
 * XML into a DOM {@link Document} ({@link RemediationXmlReader}), map the document into the
 * domain model with zero business logic ({@link RemediationDocumentMapper}), then classify
 * and apply each remediation ({@link #classifyAndApply}). Preview lists declared remediations.xml
 * fields without running the applier.
 *
 * <p><b>Per-FPR isolation:</b> Each processor instance handles exactly one FPR. In multi-FPR
 * scenarios (e.g., --all-open-issues), each artifact gets its own processor with a fresh
 * {@link AppliedChangeLedger}. Ledgers are NOT shared across artifacts because line numbers
 * are relative to pristine-file coordinates, and different artifacts are scans of potentially
 * different source revisions. Correctness across FPRs is maintained by anchor verification
 * (hash checking): once an earlier FPR has touched a file, the declared hash no longer matches,
 * so later hunks apply only where their OriginalCode still literally matches.
 */
public class RemediationProcessor {
    private static final String NAMESPACE_URI = "xmlns://www.fortify.com/schema/remediations";
    private static final Logger LOG = LoggerFactory.getLogger(RemediationProcessor.class);

    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    private final RemediationProcessingOptions options;
    private final ISourceDecoder sourceDecoder;

    private final RemediationXmlReader xmlReader = new RemediationXmlReader();
    private final RemediationDocumentMapper documentMapper = new RemediationDocumentMapper();
    private final HunkClassifier hunkClassifier = new HunkClassifier();
    private final RemediationApplier remediationApplier = new RemediationApplier();
    private final FileWriteCoordinator fileWriteCoordinator;

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory) {
        this(fprHandle, sourceCodeDirectory, SourceDecoders.defaults());
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, ISourceDecoder sourceDecoder) {
        this(fprHandle, sourceCodeDirectory,
            new RemediationProcessingOptions(Set.of(), RemediationExecutionMode.APPLY, sourceDecoder));
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, RemediationProcessingOptions options) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.options = Objects.requireNonNull(options, "options");
        this.sourceDecoder = options.sourceDecoder();
        this.fileWriteCoordinator = new FileWriteCoordinator(sourceDecoder, remediationApplier);
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Path sourceBasePath = resolveSourceBasePath();
        LOG.debug("{} remediations from {} to source directory {}",
            options.isPreview() ? "Previewing" : "Applying", remediationPath, sourceBasePath);
        FVDLMetadata fvdlMetadata = loadFvdlMetadata();

        try {
            Document remediationDoc = xmlReader.read(remediationPath);
            RemediationDocument remediations = documentMapper.map(remediationDoc);
            RemediationProcessingState state = new RemediationProcessingState(options);
            recordDescriptions(remediationDoc, state);
            if (options.isPreview()) {
                return previewRemediations(remediations, sourceBasePath, fvdlMetadata, state);
            }
            AppliedChangeLedger ledger = new AppliedChangeLedger();
            return classifyAndApply(remediations, sourceBasePath, fvdlMetadata, ledger, state);
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error processing remediation.xml: {}", remediationPath, e);
            throw new AviatorTechnicalException("Unexpected error processing remediations.xml.", e);
        }
    }

    private Path resolveSourceBasePath() {
        String trimmedSourceDir = sourceCodeDirectory.trim();
        if (trimmedSourceDir.length() > 1 &&
            ((trimmedSourceDir.startsWith("\"") && trimmedSourceDir.endsWith("\"")) ||
             (trimmedSourceDir.startsWith("'") && trimmedSourceDir.endsWith("'")))) {
            trimmedSourceDir = trimmedSourceDir.substring(1, trimmedSourceDir.length() - 1);
        }
        return Paths.get(trimmedSourceDir).toAbsolutePath().normalize();
    }

    private RemediationMetric previewRemediations(RemediationDocument remediationDocument, Path sourceBasePath,
            FVDLMetadata fvdlMetadata, RemediationProcessingState state) {
        List<Remediation> remediations = remediationDocument.remediations();
        state.setXmlEntryCount(remediations.size());
        LOG.debug("Previewing {} remediation entries from remediations.xml", remediations.size());
        for (Remediation remediation : remediations) {
            String instanceId = remediation.instanceId();
            if (!state.shouldProcess(instanceId)) {
                continue;
            }
            state.recordSeen(instanceId);
            try {
                Map<String, FilePreview> files = xmlFilePreviews(remediation, sourceBasePath, fvdlMetadata);
                state.recordPreviewAvailable(instanceId, files);
            } catch (SkipRemediationException e) {
                state.recordSkipped(instanceId, skipReasonLabel(e));
                LOG.warn("Skipping remediation {}: {}", instanceId, e.getMessage());
                LOG.debug("Skip reason for remediation {}: {}", instanceId, e.getReason().displayName(), e);
            } catch (Exception e) {
                state.recordSkipped(instanceId, SkipReason.UNEXPECTED_ERROR);
                LOG.warn("Skipping remediation {} due to an unexpected processing error", instanceId);
                LOG.debug("Unexpected error while previewing remediation {}", instanceId, e);
            }
        }
        return finish(state);
    }

    private Map<String, FilePreview> xmlFilePreviews(Remediation remediation, Path sourceBasePath, FVDLMetadata fvdlMetadata) {
        List<FileChange> fileChanges = remediation.fileChanges();
        if (fileChanges.isEmpty()) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No file changes found");
        }
        Map<String, FilePreview> files = new LinkedHashMap<>();
        for (FileChange fileChange : fileChanges) {
            String filename = fileChange.requiredFilename();
            Path filePath = fileChange.resolve(sourceBasePath);
            if (!filePath.startsWith(sourceBasePath)) {
                throw new SkipRemediationException(SkipReason.SOURCE_FILE_OUTSIDE_SOURCE_DIR,
                    "Source file resolves outside source directory: " + filename);
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new SkipRemediationException(SkipReason.SOURCE_FILE_MISSING,
                    "Source code file not present at: " + filePath);
            }
            List<Hunk> hunks = fileChange.hunks();
            if (hunks.isEmpty()) {
                throw new SkipRemediationException(SkipReason.NO_CHANGES, "No changes found for file: " + filename);
            }
            String encoding = fileWriteCoordinator.encodingFor(filePath, filename, fvdlMetadata).name();
            List<PreviewFileChange> changes = new ArrayList<>();
            FilePreview existing = files.get(filename);
            if (existing != null) {
                changes.addAll(existing.changes());
            }
            int changeIndex = changes.size();
            for (Hunk hunk : hunks) {
                changes.add(ChangeDetail.builder()
                    .changeIndex(++changeIndex)
                    .lineFrom(hunk.lineFrom())
                    .lineTo(hunk.lineTo())
                    .originalCode(hunk.requiredOriginalCode())
                    .newCode(hunk.requiredNewCode())
                    .contextLinesBefore(hunk.contextBeforeOrZero())
                    .contextLinesAfter(hunk.contextAfterOrZero())
                    .contextContent(hunk.contextTextOrEmpty())
                    .build()
                    .toPreviewFileChange());
            }
            files.put(filename, new FilePreview(filename, encoding, List.copyOf(changes)));
        }
        return files;
    }

    private RemediationMetric classifyAndApply(RemediationDocument remediationDocument, Path sourceBasePath, FVDLMetadata fvdlMetadata,
            AppliedChangeLedger ledger, RemediationProcessingState state) {
        List<Remediation> orderedRemediations = new ArrayList<>(remediationDocument.remediations());
        int totalRemediations = orderedRemediations.size();
        state.setXmlEntryCount(totalRemediations);
        LOG.debug("Loaded {} remediation entries", totalRemediations);
        try {
            // Widest-first ordering: broader fixes land first so narrower nested ones classify as SUPERSEDED.
            orderedRemediations.sort((a, b) -> Integer.compare(b.maxHunkWidth(), a.maxHunkWidth()));
        } catch (SkipRemediationException e) {
            LOG.debug("Unable to sort remediations by width; proceeding with original order", e);
        }
        Map<RemediationKey, String> remediationLookup = new LinkedHashMap<>();

        for (Remediation remediation : orderedRemediations) {
            String instanceId = remediation.instanceId();
            if (!state.shouldProcess(instanceId)) {
                continue;
            }
            state.recordSeen(instanceId);
            List<RemediationKey> remediationKeys;
            try {
                remediationKeys = remediation.createRemediationKeys(sourceBasePath);
            } catch (SkipRemediationException e) {
                state.recordSkipped(instanceId, skipReasonLabel(e));
                LOG.warn("Skipping remediation {}: {}", instanceId, e.getMessage());
                LOG.debug("Skip reason for remediation {}: {}", instanceId, e.getReason().displayName(), e);
                continue;
            }

            // Hunk-level identity: partition keys into already-satisfied vs to-apply.
            Set<RemediationKey> satisfiedKeys = new LinkedHashSet<>();
            Set<RemediationKey> toApplyKeys = new LinkedHashSet<>();
            Set<String> satisfiedByInstances = new LinkedHashSet<>();
            for (RemediationKey key : remediationKeys) {
                String owner = remediationLookup.get(key);
                if (owner != null) {
                    satisfiedKeys.add(key);
                    satisfiedByInstances.add(owner);
                } else {
                    toApplyKeys.add(key);
                }
            }

            // Fully identical: every hunk was already applied by an earlier remediation with same content.
            if (!remediationKeys.isEmpty() && toApplyKeys.isEmpty()) {
                state.recordIdentical(instanceId);
                LOG.info("Remediation {} is fully identical to prior remediation(s) {}; {} hunk(s) already applied",
                    instanceId, satisfiedByInstances, satisfiedKeys.size());
                continue;
            }

            // SUPERSEDED / CONFLICTS pre-check: classify each unsatisfied hunk against the ledger.
            List<HunkOutcome> preClass = hunkClassifier.classifyRemediationHunks(remediation, sourceBasePath, ledger);
            boolean anyApplyCandidate = preClass.stream().anyMatch(o -> o == HunkOutcome.APPLIED);
            boolean allSuperseded = !preClass.isEmpty() && preClass.stream().allMatch(o -> o == HunkOutcome.SUPERSEDED);
            boolean anyConflicts = preClass.stream().anyMatch(o -> o == HunkOutcome.CONFLICTS);
            boolean allPossiblyRemediated = !preClass.isEmpty()
                && preClass.stream().noneMatch(o -> o == HunkOutcome.APPLIED || o == HunkOutcome.CONFLICTS)
                && preClass.stream().anyMatch(o -> o == HunkOutcome.POSSIBLY_REMEDIATED);

            if (!anyApplyCandidate && allSuperseded) {
                state.recordSuperseded(instanceId);
                LOG.info("Remediation {} is superseded by a broader prior fix for all {} hunk(s); no write needed",
                    instanceId, preClass.size());
                continue;
            }
            // Remediations are applied atomically: even one CONFLICTS hunk means this remediation
            // cannot be fully/correctly applied, so reject it now rather than let the applier's
            // best-effort offset/fuzzy-anchor recovery (meant for non-conflicting shifts) decide.
            if (anyConflicts) {
                state.recordSkipped(instanceId, SkipReason.CONFLICTS_WITH_ANOTHER_FIX);
                LOG.info("Remediation {} conflicts with prior fix(es) on {} of {} hunk(s); skipping",
                    instanceId, preClass.stream().filter(o -> o == HunkOutcome.CONFLICTS).count(), preClass.size());
                continue;
            }
            if (!anyApplyCandidate && allPossiblyRemediated) {
                state.recordPossiblyRemediated(instanceId);
                LOG.info("Remediation {} possibly remediated by a sibling fix with different content for all {} hunk(s)",
                    instanceId, preClass.size());
                continue;
            }

            // Partial identity: some hunks already applied; apply only the rest.
            if (!satisfiedKeys.isEmpty()) {
                LOG.info("Remediation {} is partially identical to prior remediation(s) {}; {} of {} hunk(s) already applied, {} still to apply",
                    instanceId, satisfiedByInstances, satisfiedKeys.size(), remediationKeys.size(), toApplyKeys.size());
            }

            // Mixed classification: a hunk already covered by a broader prior fix (SUPERSEDED or
            // POSSIBLY_REMEDIATED) must not be re-attempted alongside a genuinely-applicable
            // sibling hunk, or the covered hunk's stale anchor drags the whole remediation down.
            // preClass and remediationKeys iterate the same fileChanges/hunks in the same order.
            boolean classifierNarrowed = false;
            for (int i = 0; i < preClass.size(); i++) {
                HunkOutcome outcome = preClass.get(i);
                if (outcome == HunkOutcome.SUPERSEDED || outcome == HunkOutcome.POSSIBLY_REMEDIATED) {
                    if (toApplyKeys.remove(remediationKeys.get(i))) {
                        classifierNarrowed = true;
                    }
                }
            }
            if (classifierNarrowed) {
                LOG.info("Remediation {} has {} hunk(s) already covered by a broader prior fix; applying only the rest",
                    instanceId, remediationKeys.size() - toApplyKeys.size() - satisfiedKeys.size());
            }

            Set<RemediationKey> filter = (satisfiedKeys.isEmpty() && !classifierNarrowed) ? null : toApplyKeys;
            PreparedFileChanges prepared = processRemediation(remediation, sourceBasePath, fvdlMetadata, state, filter, ledger);
            if (prepared != null && !prepared.appliedKeys().isEmpty()) {
                state.recordApplied(instanceId);
                for (RemediationKey key : prepared.appliedKeys()) {
                    LOG.debug("putting {}", instanceId);
                    remediationLookup.put(key, instanceId);
                }
            }
        }

        return finish(state);
    }

    private RemediationMetric finish(RemediationProcessingState state) {
        RemediationMetric metric = state.toMetric();
        LOG.info("Auto-remediation summary: total={}, applied={}, identical={}, superseded={}, possiblyRemediated={}, skipped={}",
            metric.totalRemediations(), metric.appliedRemediations(), metric.identicalRemediations(), metric.supersededRemediations(),
            metric.possiblyRemediatedRemediations(), metric.skippedRemediations());
        if (!metric.skippedByReason().isEmpty()) {
            LOG.info("Skipped remediations by reason: {}", formatSkippedReasons(metric.skippedByReason()));
        }
        return metric;
    }

    private PreparedFileChanges processRemediation(Remediation remediation, Path sourceBasePath, FVDLMetadata fvdlMetadata,
            RemediationProcessingState state, Set<RemediationKey> keysToApply, AppliedChangeLedger ledger) {
        String instanceId = remediation.instanceId();
        ledger.discardStaged();
        try {
            PreparedFileChanges prepared = fileWriteCoordinator.prepareFileChanges(remediation, sourceBasePath, fvdlMetadata, keysToApply, ledger);
            Map<Path, PendingFileWrite> pendingWrites = prepared.pendingWrites();

            if (pendingWrites.isEmpty()) {
                state.recordSkipped(instanceId, SkipReason.NO_CHANGES);
                return null;
            }
            try {
                fileWriteCoordinator.commitRemediationWrites(instanceId, pendingWrites, state.modifiedFiles());
                ledger.commitStaged();
                return prepared;
            } catch (RemediationCommitException e) {
                ledger.discardStaged();
                fileWriteCoordinator.rollbackRemediationWrites(instanceId, e.getRollbacks());
                throw new SkipRemediationException(SkipReason.SOURCE_WRITE_FAILED, e.getMessage(), e);
            }
        } catch (SkipRemediationException e) {
            ledger.discardStaged();
            state.recordSkipped(instanceId, skipReasonLabel(e));
            LOG.warn("Skipping remediation {}: {}", instanceId, e.getMessage());
            LOG.debug("Skip reason for remediation {}: {}", instanceId, e.getReason().displayName(), e);
            return null;
        } catch (RollbackRemediationException e) {
            throw e;
        } catch (Exception e) {
            ledger.discardStaged();
            state.recordSkipped(instanceId, SkipReason.UNEXPECTED_ERROR);
            LOG.warn("Skipping remediation {} due to an unexpected processing error", instanceId);
            LOG.debug("Unexpected error while processing remediation {}", instanceId, e);
            return null;
        }
    }

    private String skipReasonLabel(SkipRemediationException exception) {
        return exception.getReason().displayName();
    }

    private String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }

    private void recordDescriptions(Document remediationDoc, RemediationProcessingState state) {
        if (!options.isPreview()) {
            return;
        }
        NodeList remediationNodes = remediationDoc.getElementsByTagNameNS(NAMESPACE_URI, "Remediation");
        for (int i = 0; i < remediationNodes.getLength(); i++) {
            Element remediation = (Element) remediationNodes.item(i);
            String instanceId = remediation.getAttribute("instanceId");
            NodeList comments = remediation.getElementsByTagNameNS(NAMESPACE_URI, "AuditComment");
            String description = comments.getLength() == 0 ? null : comments.item(0).getTextContent();
            state.recordDescription(instanceId, description);
        }
    }

    /** Nullable: missing/unreadable FVDL means FPR encoding candidate is skipped. */
    private FVDLMetadata loadFvdlMetadata() {
        if (!Files.exists(fprHandle.getPath("/audit.fvdl"))) {
            LOG.warn("FVDL file '/audit.fvdl' is missing; FPR encoding candidate will be skipped");
            return null;
        }

        try (InputStream inputStream = Files.newInputStream(fprHandle.getPath("/audit.fvdl"))) {
            LOG.debug("Loading FVDL build metadata from '{}' to resolve source encodings", fprHandle.getFprPath());
            // Decoder unused for metadata-only parse; ctor requires one for FileUtils wiring.
            StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle, sourceDecoder);
            processor.parseBuildMetadata(inputStream);
            LOG.debug("Loaded FVDL build metadata from '{}'", fprHandle.getFprPath());
            return processor.getFvdlMetadata();
        } catch (Exception e) {
            LOG.warn("Error reading source file encodings from audit.fvdl; FPR encoding candidate will be skipped", e);
            return null;
        }
    }
}
