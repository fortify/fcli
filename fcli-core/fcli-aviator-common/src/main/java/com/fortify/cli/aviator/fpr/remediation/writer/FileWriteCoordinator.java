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
package com.fortify.cli.aviator.fpr.remediation.writer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.remediation.SkipReason;
import com.fortify.cli.aviator.fpr.remediation.applier.RemediationApplier;
import com.fortify.cli.aviator.fpr.remediation.classifier.AppliedChangeLedger;
import com.fortify.cli.aviator.fpr.remediation.classifier.PendingAppliedChange;
import com.fortify.cli.aviator.fpr.remediation.exception.RemediationCommitException;
import com.fortify.cli.aviator.fpr.remediation.exception.SkipRemediationException;
import com.fortify.cli.aviator.fpr.remediation.model.FileChange;
import com.fortify.cli.aviator.fpr.remediation.model.Hunk;
import com.fortify.cli.aviator.fpr.remediation.model.Remediation;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationKey;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.DecodeResult;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder.SourceDecodeException;
import com.fortify.cli.aviator.fpr.utils.SourceEncoder;
import com.fortify.cli.aviator.fpr.utils.SourceEncoder.SourceEncodeException;
import com.fortify.cli.aviator.util.FileUtil;

/** Houses the original prepareFileChanges/processFileChanges/commit/rollback/read/encode logic, unmodified. */
public final class FileWriteCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(FileWriteCoordinator.class);

    private final ISourceDecoder sourceDecoder;
    private final RemediationApplier remediationApplier;

    public FileWriteCoordinator(ISourceDecoder sourceDecoder, RemediationApplier remediationApplier) {
        this.sourceDecoder = sourceDecoder;
        this.remediationApplier = remediationApplier;
    }

    public PreparedFileChanges prepareFileChanges(Remediation remediation, Path sourceBasePath, FVDLMetadata fvdlMetadata,
            Set<RemediationKey> keysToApply, AppliedChangeLedger ledger) {
        String instanceId = remediation.instanceId();
        List<FileChange> fileChanges = remediation.fileChanges();
        if (fileChanges.isEmpty()) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No file changes found");
        }

        Map<Path, PendingFileWrite> pendingWrites = new LinkedHashMap<>();
        Set<RemediationKey> appliedKeys = new LinkedHashSet<>();
        SkipRemediationException firstFailure = null;
        for (int j = 0; j < fileChanges.size(); j++) {
            int appliedChangesMark = ledger.stagedMark();
            Set<RemediationKey> fileAppliedKeys = new LinkedHashSet<>();
            try {
                processFileChanges(remediation, fileChanges.get(j), sourceBasePath, fvdlMetadata, pendingWrites, keysToApply,
                    fileAppliedKeys, ledger);
                appliedKeys.addAll(fileAppliedKeys);
            } catch (SkipRemediationException e) {
                // Fix: a failure applying ONE file's hunk(s) in a multi-file remediation must not
                // discard otherwise-valid fixes already staged for OTHER files in the same remediation.
                // Roll back only this file's partial staging (it never reached pendingWrites) and continue.
                ledger.discardStagedSince(appliedChangesMark);
                if (firstFailure == null) {
                    firstFailure = e;
                }
                LOG.warn("Remediation {}: file change {}/{} could not be applied ({}); other file(s) in this remediation, if any, are still attempted",
                    instanceId, j + 1, fileChanges.size(), e.getMessage());
            }
        }
        if (pendingWrites.isEmpty() && firstFailure != null) {
            throw firstFailure;
        }
        return new PreparedFileChanges(pendingWrites, appliedKeys);
    }

    private boolean processFileChanges(Remediation remediation, FileChange fileChange, Path sourceBasePath, FVDLMetadata fvdlMetadata,
            Map<Path, PendingFileWrite> pendingWrites, Set<RemediationKey> keysToApply, Set<RemediationKey> appliedKeysOut,
            AppliedChangeLedger ledger) {

        String instanceId = remediation.instanceId();
        String filename = fileChange.requiredFilename();
        Path filePath = fileChange.resolve(sourceBasePath);
        LOG.debug("Processing remediation {} file change for '{}' resolved to '{}'", instanceId, filename, filePath);

        if (!filePath.startsWith(sourceBasePath)) {
            throw new SkipRemediationException(SkipReason.SOURCE_FILE_OUTSIDE_SOURCE_DIR,
                    "Source file resolves outside source directory: " + filename);
        }

        if (!isFilePresent(filePath)) {
            throw new SkipRemediationException(SkipReason.SOURCE_FILE_MISSING, "Source code file not present at: " + filePath);
        }

        String fileHash = fileChange.requiredHash();
        List<Hunk> hunks = fileChange.hunks();
        if (hunks.isEmpty()) {
            throw new SkipRemediationException(SkipReason.NO_CHANGES, "No changes found for file: " + filename);
        }
        SourceFileContent sourceFileContent = getPendingOrSourceContent(filePath, filename, fvdlMetadata, pendingWrites);
        Charset sourceEncoding = sourceFileContent.charset();
        LOG.debug("Remediation {} has {} change(s) for '{}' using source encoding {}", instanceId, hunks.size(), filename,
            sourceFileContent.encodingSource());

        String updatedContent = sourceFileContent.content();
        int appliedInThisFile = 0;
        int skippedAlreadySatisfied = 0;
        for (int k = 0; k < hunks.size(); k++) {
            Hunk hunk = hunks.get(k);
            String newCode = hunk.requiredNewCode();
            String comparisonCode = hunk.comparisonCode(filename);
            RemediationKey key = RemediationKey.of(fileChange, hunk, sourceBasePath, comparisonCode);
            if (keysToApply != null && !keysToApply.contains(key)) {
                LOG.info("Skipping hunk {} of remediation {} in '{}': already applied by prior identical hunk",
                    k + 1, instanceId, filename);
                skippedAlreadySatisfied++;
                continue;
            }
            int declaredLineFrom = hunk.lineFrom();
            int declaredLineTo = hunk.lineTo();
            updatedContent = remediationApplier.applyChange(instanceId, filename, filePath, fileHash, sourceEncoding, updatedContent,
                hunk, k + 1, ledger);
            // Stage this hunk into the per-run offset map (merged on commit success).
            int origLines = declaredLineTo - declaredLineFrom + 1;
            String newCodeText = FileUtil.stripSyntheticLineMarkers(newCode, filename);
            int newLines = newCodeText.isEmpty() ? 0 : newCodeText.split("\n", -1).length;
            int delta = newLines - origLines;
            ledger.stage(new PendingAppliedChange(filePath, instanceId, declaredLineFrom, declaredLineTo, delta, comparisonCode));
            appliedKeysOut.add(key);
            appliedInThisFile++;
        }
        if (appliedInThisFile == 0) {
            LOG.debug("Remediation {} produced no new hunks for '{}' ({} already satisfied); no write staged",
                instanceId, filename, skippedAlreadySatisfied);
            return true;
        }
        byte[] updatedBytes = encodeSourceFile(updatedContent, sourceEncoding, filename);

        pendingWrites.put(filePath, new PendingFileWrite(filename, filePath, updatedContent, sourceEncoding,
            sourceFileContent.encodingSource(), updatedBytes));
        LOG.debug("Staged remediation {} for '{}' using source encoding {}; changes={}, encodedBytes={}", instanceId, filename,
            sourceFileContent.encodingSource(), hunks.size(), updatedBytes.length);
        return true;
    }

    public void commitRemediationWrites(String instanceId, Map<Path, PendingFileWrite> pendingWrites, Set<String> modifiedFiles)
            throws RemediationCommitException {
        List<RollbackFileWrite> rollbacks = new ArrayList<>();
        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            try {
                byte[] originalBytes = Files.readAllBytes(pendingWrite.filePath());
                rollbacks.add(new RollbackFileWrite(pendingWrite.filename(), pendingWrite.filePath(), originalBytes));
                LOG.debug("Writing remediation {} to '{}' using staged bytes; encodedBytes={}", instanceId, pendingWrite.filename(),
                        pendingWrite.updatedBytes().length);
                Files.write(pendingWrite.filePath(), pendingWrite.updatedBytes());
            } catch (Exception e) {
                throw new RemediationCommitException("Error writing source code file '" + pendingWrite.filename() + "'", e, rollbacks);
            }
        }

        for (PendingFileWrite pendingWrite : pendingWrites.values()) {
            modifiedFiles.add(pendingWrite.filename());
            LOG.info("Remediation applied for {} in file {}", instanceId, pendingWrite.filename());
        }
    }

    public void rollbackRemediationWrites(String instanceId, List<RollbackFileWrite> rollbacks) {
        for (RollbackFileWrite rollback : rollbacks) {
            try {
                Files.write(rollback.filePath(), rollback.originalBytes());
                LOG.warn("Rolled back remediation {} changes for '{}' after write failure", instanceId, rollback.filename());
            } catch (IOException rollbackException) {
                LOG.error("Failed to roll back remediation {} changes for '{}'", instanceId, rollback.filename(), rollbackException);
                throw new com.fortify.cli.aviator.fpr.remediation.exception.RollbackRemediationException(
                        "Failed to roll back remediation changes for '" + rollback.filename() +
                        "'. Source files may be partially modified; inspect the source tree before retrying", rollbackException);
            }
        }
    }

    private SourceFileContent getPendingOrSourceContent(Path filePath, String filename, FVDLMetadata fvdlMetadata,
            Map<Path, PendingFileWrite> pendingWrites) {
        PendingFileWrite pendingWrite = pendingWrites.get(filePath);
        return pendingWrite == null
                ? readSourceFile(filePath, filename, fvdlMetadata)
                : new SourceFileContent(pendingWrite.content(), pendingWrite.charset(), pendingWrite.encodingSource());
    }

    private boolean isFilePresent(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private SourceFileContent readSourceFile(Path filePath, String filename, FVDLMetadata fvdlMetadata) {
        try {
            byte[] sourceBytes = Files.readAllBytes(filePath);
            // Metadata may be null (FVDL missing); FPR candidate fails and other encodings are tried.
            DecodeResult decodeResult = sourceDecoder.decode(sourceBytes, filename, fvdlMetadata);
            LOG.debug("Strict decoded '{}' using {}; sourceBytes={}, decodedChars={}", filename, decodeResult.source(), sourceBytes.length,
                    decodeResult.content().length());
            return new SourceFileContent(decodeResult.content(), decodeResult.charset(), decodeResult.source());
        } catch (SourceDecodeException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_DECODE_FAILED, e.getMessage(), e);
        } catch (IOException e) {
            throw new SkipRemediationException(SkipReason.SOURCE_READ_FAILED, "Error reading source code file '" + filePath + "'", e);
        }
    }

    private byte[] encodeSourceFile(String content, Charset charset, String filename) {
        try {
            return SourceEncoder.encode(content, charset, filename);
        } catch (SourceEncodeException e) {
            throw new SkipRemediationException(SkipReason.REMEDIATION_ENCODE_FAILED, e.getMessage(), e);
        }
    }
}
