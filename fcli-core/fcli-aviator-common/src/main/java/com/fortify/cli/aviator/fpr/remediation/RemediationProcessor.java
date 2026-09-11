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
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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
 * and apply each remediation ({@link #classifyAndApply}). Public API (constructors, method
 * signature) is unchanged from the original single-class implementation.
 */
public class RemediationProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(RemediationProcessor.class);

    private final FprHandle fprHandle;
    private final String sourceCodeDirectory;
    private final ISourceDecoder sourceDecoder;

    private final RemediationXmlReader xmlReader = new RemediationXmlReader();
    private final RemediationDocumentMapper documentMapper = new RemediationDocumentMapper();
    private final HunkClassifier hunkClassifier = new HunkClassifier();
    private final AppliedChangeLedger ledger = new AppliedChangeLedger();
    private final RemediationApplier remediationApplier = new RemediationApplier();
    private final FileWriteCoordinator fileWriteCoordinator;

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory) {
        this(fprHandle, sourceCodeDirectory, SourceDecoders.defaults());
    }

    public RemediationProcessor(FprHandle fprHandle, String sourceCodeDirectory, ISourceDecoder sourceDecoder) {
        this.fprHandle = fprHandle;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.sourceDecoder = Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        this.fileWriteCoordinator = new FileWriteCoordinator(sourceDecoder, remediationApplier);
    }

    public RemediationMetric processRemediationXML() {
        Path remediationPath = fprHandle.getPath("/remediations.xml");
        Path sourceBasePath = resolveSourceBasePath();
        LOG.debug("Applying remediations from {} to source directory {}", remediationPath, sourceBasePath);
        FVDLMetadata fvdlMetadata = loadFvdlMetadata();

        try {
            Document remediationDoc = xmlReader.read(remediationPath);
            RemediationDocument remediations = documentMapper.map(remediationDoc);
            return classifyAndApply(remediations, sourceBasePath, fvdlMetadata);
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

    private RemediationMetric classifyAndApply(RemediationDocument remediationDocument, Path sourceBasePath, FVDLMetadata fvdlMetadata) {
        List<Remediation> orderedRemediations = new ArrayList<>(remediationDocument.remediations());
        int totalRemediations = orderedRemediations.size();
        LOG.debug("Loaded {} remediation entries", totalRemediations);
        int appliedRemediations = 0;
        int identicalRemediations = 0;
        int supersededRemediations = 0;
        int possiblyRemediatedRemediations = 0;
        Set<String> modifiedFiles = new LinkedHashSet<>();
        Map<String, Integer> skippedByReason = new LinkedHashMap<>();
        Map<RemediationKey, String> remediationLookup = new LinkedHashMap<>();

        // Widest-first ordering: broader fixes land first so narrower nested ones classify as SUPERSEDED.
        orderedRemediations.sort((a, b) -> Integer.compare(maxHunkWidth(b), maxHunkWidth(a)));

        for (Remediation remediation : orderedRemediations) {
            String instanceId = remediation.instanceId();
            List<RemediationKey> remediationKeys = createRemediationKeys(remediation, sourceBasePath);

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
                identicalRemediations++;
                LOG.info("Remediation {} is fully identical to prior remediation(s) {}; {} hunk(s) already applied",
                    instanceId, satisfiedByInstances, satisfiedKeys.size());
                continue;
            }

            // SUPERSEDED / CONFLICTS pre-check: classify each unsatisfied hunk against the ledger.
            List<HunkOutcome> preClass = hunkClassifier.classifyRemediationHunks(remediation, sourceBasePath, ledger);
            boolean anyApplyCandidate = preClass.stream().anyMatch(o -> o == HunkOutcome.APPLIED);
            boolean allSuperseded = !preClass.isEmpty() && preClass.stream().allMatch(o -> o == HunkOutcome.SUPERSEDED);
            boolean allConflicts = !preClass.isEmpty() && preClass.stream().allMatch(o -> o == HunkOutcome.CONFLICTS);
            boolean allPossiblyRemediated = !preClass.isEmpty()
                && preClass.stream().noneMatch(o -> o == HunkOutcome.APPLIED || o == HunkOutcome.CONFLICTS)
                && preClass.stream().anyMatch(o -> o == HunkOutcome.POSSIBLY_REMEDIATED);

            if (!anyApplyCandidate && allSuperseded) {
                supersededRemediations++;
                LOG.info("Remediation {} is superseded by a broader prior fix for all {} hunk(s); no write needed",
                    instanceId, preClass.size());
                continue;
            }
            if (!anyApplyCandidate && allConflicts) {
                recordSkipped(skippedByReason, SkipReason.CONFLICTS_WITH_ANOTHER_FIX.displayName());
                LOG.info("Remediation {} conflicts with prior fix(es) on all {} hunk(s); skipping",
                    instanceId, preClass.size());
                continue;
            }
            if (!anyApplyCandidate && allPossiblyRemediated) {
                possiblyRemediatedRemediations++;
                LOG.info("Remediation {} possibly remediated by a sibling fix with different content for all {} hunk(s)",
                    instanceId, preClass.size());
                continue;
            }

            // Partial identity: some hunks already applied; apply only the rest.
            if (!satisfiedKeys.isEmpty()) {
                LOG.info("Remediation {} is partially identical to prior remediation(s) {}; {} of {} hunk(s) already applied, {} still to apply",
                    instanceId, satisfiedByInstances, satisfiedKeys.size(), remediationKeys.size(), toApplyKeys.size());
            }

            Set<RemediationKey> filter = satisfiedKeys.isEmpty() ? null : toApplyKeys;
            Set<RemediationKey> applied = processRemediation(remediation, sourceBasePath, fvdlMetadata, modifiedFiles, skippedByReason, filter);
            if (!applied.isEmpty()) {
                appliedRemediations++;
                for (RemediationKey key : applied) {
                    LOG.debug("putting {}", instanceId);
                    remediationLookup.put(key, instanceId);
                }
            }
        }

        int skippedRemediations = totalRemediations - appliedRemediations - identicalRemediations - supersededRemediations
            - possiblyRemediatedRemediations;
        LOG.info("Auto-remediation summary: total={}, applied={}, identical={}, superseded={}, possiblyRemediated={}, skipped={}",
            totalRemediations, appliedRemediations, identicalRemediations, supersededRemediations, possiblyRemediatedRemediations,
            skippedRemediations);
        if (!skippedByReason.isEmpty()) {
            LOG.info("Skipped remediations by reason: {}", formatSkippedReasons(skippedByReason));
        }
        return new RemediationMetric(totalRemediations, appliedRemediations, identicalRemediations,
            supersededRemediations, possiblyRemediatedRemediations, skippedRemediations, modifiedFiles, skippedByReason);
    }

    private Set<RemediationKey> processRemediation(Remediation remediation, Path sourceBasePath, FVDLMetadata fvdlMetadata,
            Set<String> modifiedFiles, Map<String, Integer> skippedByReason, Set<RemediationKey> keysToApply) {
        String instanceId = remediation.instanceId();
        ledger.discardStaged();
        try {
            PreparedFileChanges prepared = fileWriteCoordinator.prepareFileChanges(remediation, sourceBasePath, fvdlMetadata, keysToApply, ledger);
            Map<Path, PendingFileWrite> pendingWrites = prepared.pendingWrites();

            if (pendingWrites.isEmpty()) {
                recordSkipped(skippedByReason, SkipReason.NO_CHANGES.displayName());
                return Set.of();
            }
            try {
                fileWriteCoordinator.commitRemediationWrites(instanceId, pendingWrites, modifiedFiles);
                // Only on successful commit do the staged hunks enter the per-run offset map.
                ledger.commitStaged();
                return prepared.appliedKeys();
            } catch (RemediationCommitException e) {
                ledger.discardStaged();
                fileWriteCoordinator.rollbackRemediationWrites(instanceId, e.getRollbacks());
                throw new SkipRemediationException(SkipReason.SOURCE_WRITE_FAILED, e.getMessage(), e);
            }
        } catch (SkipRemediationException e) {
            ledger.discardStaged();
            recordSkipped(skippedByReason, skipReasonLabel(e));
            LOG.warn("Skipping remediation {}: {}", instanceId, e.getMessage());
            LOG.debug("Skip reason for remediation {}: {}", instanceId, e.getReason().displayName(), e);
            return Set.of();
        } catch (RollbackRemediationException e) {
            throw e;
        } catch (Exception e) {
            ledger.discardStaged();
            recordSkipped(skippedByReason, SkipReason.UNEXPECTED_ERROR.displayName());
            LOG.warn("Skipping remediation {} due to an unexpected processing error", instanceId);
            LOG.debug("Unexpected error while processing remediation {}", instanceId, e);
            return Set.of();
        }
    }

    private void recordSkipped(Map<String, Integer> skippedByReason, String reason) {
        skippedByReason.merge(reason, 1, Integer::sum);
    }

    private String skipReasonLabel(SkipRemediationException exception) {
        return exception.getReason().displayName();
    }

    private String formatSkippedReasons(Map<String, Integer> skippedByReason) {
        List<String> parts = new ArrayList<>();
        skippedByReason.forEach((reason, count) -> parts.add(reason + "=" + count));
        return String.join(", ", parts);
    }

    /**
     * Widest hunk (lineTo - lineFrom) across all FileChanges/Hunks in a Remediation. Used to
     * order remediations broader-first so nested narrower fixes classify as SUPERSEDED.
     */
    private int maxHunkWidth(Remediation remediation) {
        int max = 0;
        for (FileChange fileChange : remediation.fileChanges()) {
            for (Hunk hunk : fileChange.hunks()) {
                try {
                    int from = hunk.lineFrom();
                    int to = hunk.lineTo();
                    max = Math.max(max, to - from);
                } catch (Exception ignore) {
                    // best-effort ordering; malformed hunks fall to the back
                }
            }
        }
        return max;
    }

    private List<RemediationKey> createRemediationKeys(Remediation remediation, Path sourceBasePath) {
        List<RemediationKey> keys = new ArrayList<>();
        for (FileChange fileChange : remediation.fileChanges()) {
            for (Hunk hunk : fileChange.hunks()) {
                String filename = fileChange.requiredFilename();
                String comparisonCode = hunk.comparisonCode(filename);
                keys.add(RemediationKey.of(fileChange, hunk, sourceBasePath, comparisonCode));
            }
        }
        return keys;
    }

    /** Nullable: missing/unreadable FVDL means FPR encoding candidate is skipped. */
    private FVDLMetadata loadFvdlMetadata() {
        if (!Files.exists(fprHandle.getPath("/audit.fvdl"))) {
            LOG.warn("FVDL file '/audit.fvdl' is missing; FPR encoding candidate will be skipped");
            return null;
        }

        try (ZipFile zipFile = new ZipFile(fprHandle.getFprPath().toFile())) {
            LOG.debug("Loading FVDL build metadata from '{}' to resolve source encodings", fprHandle.getFprPath());
            // Decoder unused for metadata-only parse; ctor requires one for FileUtils wiring.
            StreamingFVDLProcessor processor = new StreamingFVDLProcessor(fprHandle, sourceDecoder);
            processor.parseBuildMetadata(zipFile, "audit.fvdl");
            LOG.debug("Loaded FVDL build metadata from '{}'", fprHandle.getFprPath());
            return processor.getFvdlMetadata();
        } catch (Exception e) {
            LOG.warn("Error reading source file encodings from audit.fvdl; FPR encoding candidate will be skipped", e);
            return null;
        }
    }
}
