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
package com.fortify.cli.aviator.ssc.cli.cmd;

import static com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper.getLatestDASTArtifact;
import static com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper.getLatestSASTArtifact;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.grpc.CorrelationResult;
import com.fortify.cli.aviator.grpc.CorrelationStreamConfig;
import com.fortify.cli.aviator.grpc.CorrelationStreamProcessor;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateDownloadHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser.ParseResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelationAttributeHelper;
import com.fortify.cli.aviator.ssc.helper.CategoryBucket;
import com.fortify.cli.aviator.ssc.helper.CategoryGrouper;
import com.fortify.cli.aviator.ssc.helper.DastFprCorrelationEnricher;
import com.fortify.cli.aviator.ssc.helper.SastFprCorrelationRecorder;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "correlate-sast-dast")
public class AviatorSSCCorrelateSastDastCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Mixin private AviatorUserSessionDescriptorSupplier sessionDescriptorSupplier;
    @Option(names = {"--app"}) private String appName;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelateSastDastCommand.class);
    private String actionResult = "CORRELATED";

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            var logger = new AviatorLoggerImpl(progressWriter);
            var av = appVersionResolver.getAppVersionDescriptor(unirest);
            var processor = new CorrelationProcessor(unirest, logger, progressWriter, av, sessionDescriptor);
            return processor.run();
        }
    }

    /**
     * Encapsulates the multi-step correlation workflow, keeping each phase
     * in a focused helper method and avoiding long parameter chains.
     */
    @RequiredArgsConstructor
    private class CorrelationProcessor {
        private final UnirestInstance unirest;
        private final AviatorLoggerImpl logger;
        private final IProgressWriter progressWriter;
        private final SSCAppVersionDescriptor av;
        private final AviatorUserSessionDescriptor sessionDescriptor;

        private record DownloadedFprs(Path sastPath, Path dastPath, SSCArtifactDescriptor adDast) {}

        JsonNode run() {
            logger.progress("Status: Starting SAST-DAST correlation for %s:%s", av.getApplicationName(), av.getVersionName());

            var fprs = downloadFprs();
            var sastResult = parseFpr(fprs.sastPath, "SAST");
            var dastResult = parseFpr(fprs.dastPath, "DAST");

            var unsuppressedSast = filterUnsuppressedSast(sastResult);
            var unsuppressedDast = filterUnsuppressedDast(dastResult);
            var alreadyTriedKeys = buildAlreadyTriedKeys(unsuppressedDast, fprs.sastPath, sastResult, dastResult);

            var mixedBuckets = groupByCategory(unsuppressedSast, unsuppressedDast);
            int submitted = countNewSastFindings(mixedBuckets, alreadyTriedKeys);

            var grpcResult = correlateViaGrpc(mixedBuckets, alreadyTriedKeys, submitted, sastResult);
            String uploadedArtifactId = uploadCorrelatedFprs(fprs, grpcResult);

            logger.progress("Status: Correlation process complete for %s:%s — result: %s",
                av.getApplicationName(), av.getVersionName(), actionResult);
            return AviatorSSCCorrelateHelper.buildOutputJson(
                av, uploadedArtifactId, submitted, grpcResult.succeeded, grpcResult.confirmed, actionResult);
        }

        private DownloadedFprs downloadFprs() {
            try {
                logger.progress("Status: Downloading SAST FPR from SSC for %s:%s", av.getApplicationName(), av.getVersionName());
                var adSast = getLatestSASTArtifact(unirest, av.getVersionId());
                var sastPath = AviatorSSCCorrelateDownloadHelper.downloadArtifactFpr(unirest, adSast, logger, progressWriter);

                logger.progress("Status: Downloading DAST FPR from SSC for %s:%s", av.getApplicationName(), av.getVersionName());
                var adDast = getLatestDASTArtifact(unirest, av.getVersionId());
                var dastPath = AviatorSSCCorrelateDownloadHelper.downloadArtifactFpr(unirest, adDast, logger, progressWriter);

                AviatorSSCCorrelateHelper.validateDownloadedFpr(sastPath, "SAST");
                AviatorSSCCorrelateHelper.validateDownloadedFpr(dastPath, "DAST");
                return new DownloadedFprs(sastPath, dastPath, adDast);
            } catch (java.io.IOException e) {
                throw new FcliSimpleException("Failed to download FPR from SSC: " + e.getMessage(), e);
            }
        }

        private ParseResult parseFpr(Path fprPath, String type) {
            logger.progress("Status: Parsing %s FPR...", type);
            return "SAST".equals(type)
                ? AviatorSSCCorrelateFprParser.parseSastFpr(fprPath)
                : AviatorSSCCorrelateFprParser.parseDastFpr(fprPath);
        }

        private List<Vulnerability> filterUnsuppressedSast(ParseResult sastResult) {
            return sastResult.vulnerabilities.stream()
                .filter(v -> !AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(v, sastResult.auditIssueMap))
                .collect(Collectors.toList());
        }

        private List<DastIssue> filterUnsuppressedDast(ParseResult dastResult) {
            return dastResult.dastIssues.stream()
                .filter(d -> !d.isSuppressed())
                .collect(Collectors.toList());
        }

        private Set<String> buildAlreadyTriedKeys(List<DastIssue> unsuppressedDast, Path sastFprPath,
                                                   ParseResult sastResult, ParseResult dastResult) {
            Set<String> confirmedPairKeys = buildPreviouslyCorrelatedPairKeys(unsuppressedDast);
            Set<String> rejectedPairKeys = SastFprCorrelationRecorder.readTriedPairKeys(sastFprPath);
            Set<String> alreadyTriedKeys = new HashSet<>(confirmedPairKeys);
            alreadyTriedKeys.addAll(rejectedPairKeys);

            LOG.info("Total SAST issues {}", sastResult.vulnerabilities.size());
            LOG.info("Total DAST issues {}", dastResult.dastIssues.size());
            LOG.info("Confirmed pairs (from ExternalFindings): {}", confirmedPairKeys.size());
            LOG.info("Pairs from DAST_CORRELATION_STATUS tag: {}", rejectedPairKeys.size());
            LOG.info("Total already-tried pairs (will be skipped): {}", alreadyTriedKeys.size());
            return alreadyTriedKeys;
        }

        private List<CategoryBucket> groupByCategory(List<Vulnerability> sast, List<DastIssue> dast) {
            logger.progress("Status: Found %d SAST and %d DAST unsuppressed issues to correlate", sast.size(), dast.size());
            logger.progress("Status: Grouping findings by vulnerability category...");
            var grouper = new CategoryGrouper();
            grouper.groupFindings(sast, dast);
            grouper.printStatistics();
            return grouper.getMixedBuckets();
        }

        private record GrpcResult(List<CorrelatedPair> confirmed, List<CorrelatedPair> rejected, int succeeded) {}

        private GrpcResult correlateViaGrpc(List<CategoryBucket> mixedBuckets, Set<String> alreadyTriedKeys,
                                            int submitted, ParseResult sastResult) {
            if (mixedBuckets.isEmpty()) {
                actionResult = "SKIPPED";
                logger.progress("Status: No mixed categories found — skipping correlation.");
                return new GrpcResult(List.of(), List.of(), 0);
            }

            logger.progress("Status: Found %d mixed category bucket(s) with %d SAST findings to correlate",
                mixedBuckets.size(), submitted);

            var bucketData = mixedBuckets.stream()
                .map(b -> new CorrelationStreamProcessor.CorrelationBucketData(
                    b.getCategory(), b.getSastFindings(), b.getDastFindings()))
                .collect(Collectors.toList());

            var config = new CorrelationStreamConfig(
                sessionDescriptor.getAviatorToken(),
                appName != null ? appName : "",
                av.getApplicationName(), av.getVersionName(), sastResult.buildId);

            List<CorrelatedPair> confirmed;
            List<CorrelatedPair> rejected;
            int succeeded;
            try (var grpcClient = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl(), logger, 30)) {
                var result = performCorrelation(grpcClient, config, bucketData, sastResult.scanGuid, alreadyTriedKeys);
                confirmed = result.confirmedPairs();
                rejected = result.rejectedPairs();
                succeeded = result.receivedCorrelationResponses();
            }

            logger.progress("Status: Correlation complete — %d of %d SAST findings confirmed as correlated",
                confirmed.size(), submitted);
            actionResult = succeeded == 0 ? "SKIPPED" : succeeded < submitted ? "PARTIALLY_CORRELATED" : "CORRELATED";
            return new GrpcResult(confirmed, rejected, succeeded);
        }

        private String uploadCorrelatedFprs(DownloadedFprs fprs, GrpcResult grpcResult) {
            String uploadedArtifactId = null;
            if (!grpcResult.confirmed.isEmpty()) {
                uploadedArtifactId = uploadEnrichedDastFpr(fprs, grpcResult.confirmed);
            } else {
                logger.progress("Status: No correlated pairs found — skipping DAST FPR upload.");
            }

            if (!grpcResult.confirmed.isEmpty() || !grpcResult.rejected.isEmpty()) {
                uploadTaggedSastFpr(fprs.sastPath, grpcResult.confirmed, grpcResult.rejected);
                writeLastCorrelationTimestamp();
            }
            return uploadedArtifactId;
        }

        private String uploadEnrichedDastFpr(DownloadedFprs fprs, List<CorrelatedPair> confirmed) {
            logger.progress("Status: Injecting correlation data into DAST FPR (%d correlated pair(s))...", confirmed.size());
            new DastFprCorrelationEnricher().injectAndRepackage(fprs.dastPath, confirmed);

            logger.progress("Status: Uploading correlated DAST FPR to SSC...");
            AviatorSSCCorrelateDownloadHelper.uploadEnrichedDastFpr(unirest, av, fprs.dastPath, progressWriter);
            String artifactId = fprs.adDast.getId();
            logger.progress("Status: Correlated DAST FPR uploaded successfully (artifact id=%s)", artifactId);
            return artifactId;
        }

        private void uploadTaggedSastFpr(Path sastPath, List<CorrelatedPair> confirmed, List<CorrelatedPair> rejected) {
            logger.progress("Status: Writing correlation status tags to SAST FPR (%d confirmed, %d rejected)...",
                confirmed.size(), rejected.size());
            SastFprCorrelationRecorder.writeCorrelationTags(sastPath, confirmed, rejected);

            logger.progress("Status: Uploading updated SAST FPR to SSC...");
            AviatorSSCCorrelateDownloadHelper.uploadEnrichedSastFpr(unirest, av, sastPath, progressWriter);
            logger.progress("Status: Updated SAST FPR uploaded successfully.");
        }

        private void writeLastCorrelationTimestamp() {
            logger.progress("Status: Writing last_correlation timestamp to app version...");
            AviatorSSCCorrelationAttributeHelper.writeLastCorrelationTimestamp(unirest, av.getVersionId());
            logger.progress("Status: last_correlation timestamp written successfully.");
        }

        private CorrelationResult performCorrelation(AviatorGrpcClient grpcClient, CorrelationStreamConfig config,
                List<CorrelationStreamProcessor.CorrelationBucketData> bucketData, String scanGuid,
                Set<String> alreadyTriedKeys) {
            try {
                var processor = new CorrelationStreamProcessor(
                    grpcClient, logger, grpcClient.getCorrelationAsyncStub(),
                    grpcClient.getPingScheduler(), grpcClient.getPingIntervalSeconds(),
                    grpcClient.getDefaultTimeoutSeconds());
                long timeoutSeconds = Math.max(grpcClient.getDefaultTimeoutSeconds(), 300);
                return processor.processCorrelation(config, bucketData, scanGuid, alreadyTriedKeys)
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new FcliSimpleException("Correlation stream timed out waiting for server responses", e);
            } catch (Exception e) {
                throw new FcliSimpleException("Correlation stream failed: " + e.getMessage(), e);
            }
        }
    }

    private Set<String> buildPreviouslyCorrelatedPairKeys(List<DastIssue> dastIssues) {
        Set<String> keys = new HashSet<>();
        for (DastIssue dastIssue : dastIssues) {
            if (dastIssue.getId() == null || dastIssue.getId().isEmpty()) continue;
            for (String sastId : dastIssue.getExistingCorrelatedSastIds()) {
                keys.add(sastId + "::" + dastIssue.getId());
            }
        }
        LOG.debug("Built {} previously-correlated pair keys from ExternalFindings", keys.size());
        return keys;
    }

    private int countNewSastFindings(List<CategoryBucket> buckets, Set<String> alreadyTriedKeys) {
        if (alreadyTriedKeys.isEmpty()) {
            return buckets.stream().mapToInt(CategoryBucket::getSastCount).sum();
        }
        int count = 0;
        for (CategoryBucket bucket : buckets) {
            for (Vulnerability sast : bucket.getSastFindings()) {
                boolean hasNewPairing = bucket.getDastFindings().stream()
                    .anyMatch(dast -> !alreadyTriedKeys.contains(sast.getInstanceID() + "::" + dast.getId()));
                if (hasNewPairing) count++;
            }
        }
        return count;
    }

    @Override
    public String getActionCommandResult() {
        return actionResult;
    }


    @Override
    public boolean isSingular() {
        return true;
    }
}
