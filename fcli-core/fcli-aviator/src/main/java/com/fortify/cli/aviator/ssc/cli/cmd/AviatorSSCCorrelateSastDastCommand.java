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

import java.io.IOException;
import java.nio.file.Files;
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
import com.fortify.cli.aviator.grpc.CorrelationResult;
import com.fortify.cli.aviator.grpc.CorrelationStreamConfig;
import com.fortify.cli.aviator.grpc.CorrelationStreamProcessor;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAttributeHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser.ParseResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCFprTransferHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCRefreshHelper;
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
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionRefreshOptions;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;

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
    @Mixin private SSCAppVersionRefreshOptions refreshOptions;
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

        private record CorrelationFiles(Path statePath, Path historyPath, boolean unchangedSinceCorrelation)
                implements AutoCloseable {
            @Override
            public void close() {
                deleteTemporaryFile(statePath);
                deleteTemporaryFile(historyPath);
            }
        }

        JsonNode run() {
            logger.progress("Status: Starting SAST-DAST correlation for %s:%s", av.getApplicationName(), av.getVersionName());

            AviatorSSCRefreshHelper.refreshMetricsIfNeeded(
                unirest, av, refreshOptions.isRefresh(), refreshOptions.getRefreshTimeout(), logger);
            try (var files = downloadCorrelationFiles()) {
                return correlate(files);
            }
        }

        private JsonNode correlate(CorrelationFiles files) {
            var sastResult = parseFpr(files.statePath(), "SAST");
            var dastResult = parseFpr(files.statePath(), "DAST");

            var unsuppressedSast = filterUnsuppressedSast(sastResult);
            var unsuppressedDast = filterUnsuppressedDast(dastResult);
            var alreadyTriedKeys = buildAlreadyTriedKeys(
                unsuppressedDast, files.statePath(), files.historyPath(), sastResult, dastResult);

            if (files.unchangedSinceCorrelation() && !alreadyTriedKeys.isEmpty()) {
                actionResult = "SKIPPED";
                logger.progress("Status: No newer SAST or DAST scan found — skipping correlation and FPR upload.");
                return AviatorSSCCorrelateHelper.buildOutputJson(
                    av, null, CorrelationResult.empty(), actionResult);
            }

            var mixedBuckets = groupByCategory(unsuppressedSast, unsuppressedDast);

            var grpcResult = correlateViaGrpc(mixedBuckets, alreadyTriedKeys, sastResult);
            String uploadedArtifactId = uploadCorrelationResults(files.statePath(), grpcResult);

            logger.progress("Status: Correlation process complete for %s:%s — result: %s",
                av.getApplicationName(), av.getVersionName(), actionResult);
            return AviatorSSCCorrelateHelper.buildOutputJson(
                av, uploadedArtifactId, grpcResult, actionResult);
        }

        private CorrelationFiles downloadCorrelationFiles() {
            Path statePath = null;
            Path historyPath = null;
            try {
                statePath = AviatorSSCFprTransferHelper.downloadCurrentStateFpr(
                    unirest, av, logger, progressWriter);
                var sastArtifact = getLatestSASTArtifact(unirest, av.getVersionId());
                var historyArtifact = getLatestDASTArtifact(unirest, av.getVersionId());
                historyPath = AviatorSSCFprTransferHelper.downloadArtifactFpr(
                    unirest, historyArtifact, logger, progressWriter);
                AviatorSSCCorrelateHelper.validateDownloadedFpr(statePath, "merged");
                AviatorSSCCorrelateHelper.validateDownloadedFpr(historyPath, "correlation history");
                boolean unchangedSinceCorrelation = AviatorSSCCorrelateHelper.isUnchangedSinceCorrelation(
                    sastArtifact, historyArtifact);
                return new CorrelationFiles(statePath, historyPath, unchangedSinceCorrelation);
            } catch (IOException e) {
                deleteTemporaryFile(statePath);
                deleteTemporaryFile(historyPath);
                throw new FcliSimpleException("Failed to download FPR from SSC: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                deleteTemporaryFile(statePath);
                deleteTemporaryFile(historyPath);
                throw e;
            }
        }

        private static void deleteTemporaryFile(Path path) {
            if (path == null) return;
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                LOG.warn("Failed to delete temporary correlation FPR {}", path, e);
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

        private Set<String> buildAlreadyTriedKeys(List<DastIssue> unsuppressedDast, Path statePath, Path historyPath,
                                                   ParseResult sastResult, ParseResult dastResult) {
            Set<String> confirmedPairKeys = buildPreviouslyCorrelatedPairKeys(unsuppressedDast);
            Set<String> statePairKeys = SastFprCorrelationRecorder.readTriedPairKeys(statePath);
            Set<String> historyPairKeys = SastFprCorrelationRecorder.readTriedPairKeys(historyPath);
            Set<String> alreadyTriedKeys = new HashSet<>(confirmedPairKeys);
            alreadyTriedKeys.addAll(statePairKeys);
            alreadyTriedKeys.addAll(historyPairKeys);

            LOG.info("Total SAST issues {}", sastResult.vulnerabilities.size());
            LOG.info("Total DAST issues {}", dastResult.dastIssues.size());
            LOG.info("Confirmed pairs (from ExternalFindings): {}", confirmedPairKeys.size());
            LOG.info("Pairs from current-state DAST_CORRELATION_STATUS tags: {}", statePairKeys.size());
            LOG.info("Pairs from latest successful DAST artifact tags: {}", historyPairKeys.size());
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

        private CorrelationResult correlateViaGrpc(List<CategoryBucket> mixedBuckets,
                               Set<String> alreadyTriedKeys,
                               ParseResult sastResult) {
            if (mixedBuckets.isEmpty()) {
                actionResult = "SKIPPED";
                logger.progress("Status: No mixed categories found — skipping correlation.");
                return CorrelationResult.empty();
            }

            logger.progress("Status: Found %d mixed category bucket(s) to correlate", mixedBuckets.size());

            var bucketData = mixedBuckets.stream()
                .map(b -> new CorrelationStreamProcessor.CorrelationBucketData(
                    b.getCategory(), b.getSastFindings(), b.getDastFindings()))
                .collect(Collectors.toList());

            var config = new CorrelationStreamConfig(
                sessionDescriptor.getAviatorToken(),
                appName != null ? appName : "",
                av.getApplicationName(), av.getVersionName(), sastResult.buildId);

            CorrelationResult result;
            try (var grpcClient = AviatorGrpcClientHelper.createClient(sessionDescriptor.getAviatorUrl(), logger, 30)) {
                result = performCorrelation(grpcClient, config, bucketData, sastResult.scanGuid, alreadyTriedKeys);
            }

            logger.progress("Status: Correlation complete — %d pairs confirmed from %d submitted SAST findings",
                result.confirmedPairs().size(), result.submittedCorrelationRequests());
            actionResult = getActionResult(result);
            return result;
        }

        private String getActionResult(CorrelationResult result) {
            int submitted = result.submittedCorrelationRequests();
            int succeeded = result.successfulCorrelationResponses();
            int skipped = result.skippedCorrelationResponses();
            int failed = result.failedCorrelationResponses();
            return submitted == 0 ? "SKIPPED"
                : failed == submitted ? "FAILED"
                : succeeded == 0 ? "SKIPPED"
                : failed > 0 || skipped > 0 ? "PARTIALLY_CORRELATED" : "CORRELATED";
        }

        private String uploadCorrelationResults(Path statePath, CorrelationResult result) {
            if (result.confirmedPairs().isEmpty() && result.rejectedPairs().isEmpty()) {
                logger.progress("Status: No correlation results found — skipping FPR upload.");
                return null;
            }

            if (!result.confirmedPairs().isEmpty()) {
                logger.progress("Status: Injecting correlation data into merged FPR (%d correlated pair(s))...",
                    result.confirmedPairs().size());
                new DastFprCorrelationEnricher().injectAndRepackage(statePath, result.confirmedPairs());
            }
            logger.progress("Status: Writing correlation status tags to merged FPR (%d confirmed, %d rejected)...",
                result.confirmedPairs().size(), result.rejectedPairs().size());
            SastFprCorrelationRecorder.writeCorrelationTags(
                statePath, result.confirmedPairs(), result.rejectedPairs());

            logger.progress("Status: Uploading correlated merged FPR to SSC...");
            String artifactId = AviatorSSCFprTransferHelper.uploadFpr(
                unirest, av, statePath, progressWriter);
            logger.progress("Status: Correlated merged FPR uploaded (artifact id=%s)", artifactId);
            logger.progress("Status: Waiting for correlated merged FPR processing...");
            AviatorSSCFprTransferHelper.waitForArtifactProcessing(unirest, artifactId);
            logger.progress("Status: Correlated merged FPR processing complete (artifact id=%s)", artifactId);

            writeLastCorrelationTimestamp();
            return artifactId;
        }

        private void writeLastCorrelationTimestamp() {
            logger.progress("Status: Writing last_correlation timestamp to app version...");
            AviatorSSCAttributeHelper.writeLastCorrelationTimestamp(unirest, av.getVersionId());
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

    @Override
    public String getActionCommandResult() {
        return actionResult;
    }


    @Override
    public boolean isSingular() {
        return true;
    }
}
