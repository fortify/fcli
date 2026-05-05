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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.grpc.*;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCAttributeHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateDownloadHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser.ParseResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateHelper;
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
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);

            logger.progress("Status: Starting SAST-DAST correlation for %s:%s", av.getApplicationName(), av.getVersionName());

            // Step 1: Download SAST and DAST FPRs from SSC
            Path downloadedSASTFprPath;
            Path downloadedDASTFprPath;
            SSCArtifactDescriptor adDast;
            try {
                logger.progress("Status: Downloading SAST FPR from SSC for %s:%s", av.getApplicationName(), av.getVersionName());
                SSCArtifactDescriptor adSast = getLatestSASTArtifact(unirest, av.getVersionId());
                downloadedSASTFprPath = AviatorSSCCorrelateDownloadHelper.downloadArtifactFpr(unirest, adSast, logger, progressWriter);

                logger.progress("Status: Downloading DAST FPR from SSC for %s:%s", av.getApplicationName(), av.getVersionName());
                adDast = getLatestDASTArtifact(unirest, av.getVersionId());
                downloadedDASTFprPath = AviatorSSCCorrelateDownloadHelper.downloadArtifactFpr(unirest, adDast, logger, progressWriter);
            } catch (java.io.IOException e) {
                throw new FcliSimpleException("Failed to download FPR from SSC: " + e.getMessage(), e);
            }

            AviatorSSCCorrelateHelper.validateDownloadedFpr(downloadedSASTFprPath, "SAST");
            AviatorSSCCorrelateHelper.validateDownloadedFpr(downloadedDASTFprPath, "DAST");

            // Step 2: Parse both FPRs
            logger.progress("Status: Parsing SAST FPR...");
            ParseResult sastResult = AviatorSSCCorrelateFprParser.parseSastFpr(downloadedSASTFprPath);
            logger.progress("Status: Parsing DAST FPR...");
            ParseResult dastResult = AviatorSSCCorrelateFprParser.parseDastFpr(downloadedDASTFprPath);

            // Step 3: Filter to unsuppressed issues only
            List<Vulnerability> unsuppressedSast = sastResult.vulnerabilities.stream()
                .filter(v -> !AviatorSSCCorrelateHelper.isVulnerabilitySuppressed(v, sastResult.auditIssueMap))
                .collect(Collectors.toList());
            List<DastIssue> unsuppressedDast = dastResult.dastIssues.stream()
                .filter(d -> !d.isSuppressed())
                .collect(Collectors.toList());

            // Build the set of already-tried pair keys:
            //   confirmedPairKeys — from <ExternalFindings> in DAST FPR webinspect.xml (authoritative)
            //   rejectedPairKeys  — from DAST_CORRELATION_STATUS tag in SAST FPR audit.xml
            Set<String> confirmedPairKeys = buildPreviouslyCorrelatedPairKeys(unsuppressedDast);
            Set<String> rejectedPairKeys  = SastFprCorrelationRecorder.readTriedPairKeys(downloadedSASTFprPath);
            Set<String> alreadyTriedKeys  = new HashSet<>(confirmedPairKeys);
            alreadyTriedKeys.addAll(rejectedPairKeys);

            LOG.info("Total SAST issues {}", sastResult.vulnerabilities.size());
            LOG.info("Total DAST issues {}", dastResult.dastIssues.size());
            LOG.info("Total Unsupressed SAST issues {}", unsuppressedSast.size());
            LOG.info("Total Unsupressed DAST issues {}", unsuppressedDast.size());
            LOG.info("Confirmed pairs (from ExternalFindings): {}", confirmedPairKeys.size());
            LOG.info("Pairs from DAST_CORRELATION_STATUS tag: {}", rejectedPairKeys.size());
            LOG.info("Total already-tried pairs (will be skipped): {}", alreadyTriedKeys.size());

            logger.progress("Status: Found %d SAST and %d DAST unsuppressed issues to correlate",
                unsuppressedSast.size(), unsuppressedDast.size());

            // Step 4: Group by category
            logger.progress("Status: Grouping findings by vulnerability category...");
            CategoryGrouper grouper = new CategoryGrouper();
            grouper.groupFindings(unsuppressedSast, unsuppressedDast);
            grouper.printStatistics();
            List<CategoryBucket> mixedBuckets = grouper.getMixedBuckets();

            // Step 5: gRPC correlation (if mixed buckets exist)
            // Count only SAST findings with at least one untried DAST pairing in their bucket
            int submitted = countNewSastFindings(mixedBuckets, alreadyTriedKeys);
            logger.info("New pairs after removing the already-tried pairs is {}", submitted);
            List<CorrelatedPair> newPairs = new ArrayList<>();
            List<CorrelatedPair> newRejectedPairs = new ArrayList<>();
            int succeeded = 0;
            if (!mixedBuckets.isEmpty()) {
                logger.progress("Status: Found %d mixed category bucket(s) with %d SAST findings to correlate",
                    mixedBuckets.size(), submitted);

                List<CorrelationStreamProcessor.CorrelationBucketData> bucketData = mixedBuckets.stream()
                    .map(b -> new CorrelationStreamProcessor.CorrelationBucketData(
                        b.getCategory(), b.getSastFindings(), b.getDastFindings()))
                    .collect(Collectors.toList());

                String aviatorUrl = sessionDescriptor.getAviatorUrl();
                String token = sessionDescriptor.getAviatorToken();

                var config = new CorrelationStreamConfig(
                    token,
                    appName != null ? appName : "",
                    av.getApplicationName(),
                    av.getVersionName(),
                    sastResult.buildId
                );

                //logger.progress("Status: Connecting to Aviator server for correlation...");
                try (AviatorGrpcClient grpcClient = AviatorGrpcClientHelper.createClient(aviatorUrl, logger, 30)) {
                    CorrelationResult result = performCorrelation(grpcClient, config, bucketData, sastResult.scanGuid, logger, alreadyTriedKeys);
                    newPairs = result.confirmedPairs();
                    newRejectedPairs = result.rejectedPairs();
                    succeeded = result.receivedCorrelationResponses();
                }

                logger.progress("Status: Correlation complete — %d of %d SAST findings confirmed as correlated",
                    newPairs.size(), submitted);

                if (succeeded==0) {
                    actionResult = "SKIPPED";
                } else if (succeeded < submitted) {
                    actionResult = "PARTIALLY_CORRELATED";
                } else {
                    actionResult = "CORRELATED";
                }
            } else {
                actionResult = "SKIPPED";
                logger.progress("Status: No mixed categories found — skipping correlation.");
                logger.info("No mixed categories found — skipping correlation.");
            }

            // Step 6: Inject <ExternalFindings> into DAST FPR and upload

            //Testing the upload flow
            /*String SAST_INSTANCE_1 = "00403DBC3662FEBAD561B1A578AE7556";
            String SAST_INSTANCE_2 = "00411ED275CA1DCF328136A99613E95E";
            String SAST_INSTANCE_3 = "0080AE7911F7A5D3A8BDEFD0DD046FB2";
            String DAST_ISSUE_1 = "b7391a4f-deb4-664d-3e23-ca2cc43a7527";
            String SAST_SCAN_GUID = "62cd94b2-523a-409e-86eb-9b55a0421380";
            List<CorrelatedPair> newPairs = List.of(
                new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Primary match"),
                new CorrelatedPair(SAST_INSTANCE_2, DAST_ISSUE_1, SAST_SCAN_GUID, "MEDIUM", "Secondary match"),
                new CorrelatedPair(SAST_INSTANCE_3, DAST_ISSUE_1, SAST_SCAN_GUID, "LOW", "Tertiary match")
            );*/

            String uploadedArtifactId = null;
            if (!newPairs.isEmpty()) {
                logger.progress("Status: Injecting correlation data into DAST FPR (%d correlated pair(s))...", newPairs.size());
                DastFprCorrelationEnricher enricher = new DastFprCorrelationEnricher();
                enricher.injectAndRepackage(downloadedDASTFprPath, newPairs);
                /*try {
                    Files.copy(downloadedDASTFprPath, Path.of("C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-dast.fpr"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("Enriched DAST FPR saved to: C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-dast.fpr");
                } catch (java.io.IOException e) {
                    LOG.warn("Failed to save enriched DAST FPR locally: {}", e.getMessage());
                }*/

                //First we have to delete the existing DAST FPR then upload the enriched DAST FPR. Only through this process
                // correlation data is available on the SSC

                logger.progress("Status: Uploading correlated DAST FPR to SSC...");
                AviatorSSCCorrelateDownloadHelper.uploadEnrichedDastFpr(unirest, av, downloadedDASTFprPath, progressWriter);
                uploadedArtifactId = adDast.getId();
                logger.progress("Status: Correlated DAST FPR uploaded successfully (artifact id=%s)", uploadedArtifactId);
            } else {
                logger.progress("Status: No correlated pairs found — skipping DAST FPR upload.");
            }

            // Step 6b: Write DAST_CORRELATION_STATUS tags into SAST FPR and re-upload
            if (!newPairs.isEmpty() || !newRejectedPairs.isEmpty()) {
                logger.progress("Status: Writing correlation status tags to SAST FPR (%d confirmed, %d rejected)...",
                    newPairs.size(), newRejectedPairs.size());
                SastFprCorrelationRecorder.writeCorrelationTags(downloadedSASTFprPath, newPairs, newRejectedPairs);
                /*try {
                    Files.copy(downloadedSASTFprPath, Path.of("C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-sast.fpr"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("Enriched SAST FPR saved to: C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-sast.fpr");
                } catch (java.io.IOException e) {
                    LOG.warn("Failed to save enriched SAST FPR locally: {}", e.getMessage());
                }*/
                logger.progress("Status: Uploading updated SAST FPR to SSC...");
                AviatorSSCCorrelateDownloadHelper.uploadEnrichedSastFpr(unirest, av, downloadedSASTFprPath, progressWriter);
                logger.progress("Status: Updated SAST FPR uploaded successfully.");

                // Step 6c: Write last_correlation timestamp to SSC app version attribute.
                // Placed after all FPR uploads so it is always later than lastScanDate.
                logger.progress("Status: Writing last_correlation timestamp to app version...");
                AviatorSSCAttributeHelper.writeLastCorrelationTimestamp(unirest, av.getVersionId());
                logger.progress("Status: last_correlation timestamp written successfully.");
            }


            // Step 7: Build output
            logger.progress("Status: Correlation process complete for %s:%s — result: %s",
                av.getApplicationName(), av.getVersionName(), actionResult);
            return AviatorSSCCorrelateHelper.buildOutputJson(av, uploadedArtifactId, submitted, succeeded, newPairs, actionResult);
        }
    }

    private CorrelationResult performCorrelation(
            AviatorGrpcClient grpcClient,
            CorrelationStreamConfig config,
            List<CorrelationStreamProcessor.CorrelationBucketData> bucketData,
            String scanGuid,
            AviatorLoggerImpl logger,
            Set<String> alreadyTriedKeys) {
        try {
            var processor = new CorrelationStreamProcessor(
                grpcClient, logger,
                grpcClient.getCorrelationAsyncStub(),
                grpcClient.getPingScheduler(),
                grpcClient.getPingIntervalSeconds(),
                grpcClient.getDefaultTimeoutSeconds()
            );
            long timeoutSeconds = Math.max(grpcClient.getDefaultTimeoutSeconds(), 300);
            return processor.processCorrelation(config, bucketData, scanGuid, alreadyTriedKeys)
                .get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new FcliSimpleException("Correlation stream timed out waiting for server responses", e);
        } catch (Exception e) {
            throw new FcliSimpleException("Correlation stream failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds a set of {@code "sastInstanceId::dastIssueId"} keys from the
     * {@code <ExternalFindings>} already present in the parsed DAST issues.
     * These pairs were confirmed in a previous correlation run and will be
     * skipped by the gRPC processor to avoid redundant server calls.
     */
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

    /**
     * Counts SAST findings across all mixed buckets that have at least one
     * uncorrelated DAST pairing in the same bucket. Already fully-correlated
     * SAST findings (every co-bucket DAST issue already confirmed) are excluded,
     * so {@code submitted} accurately reflects the new work being sent to the
     * gRPC server.
     */
    private int countNewSastFindings(List<CategoryBucket> buckets, Set<String> alreadyTriedKeys) {
        LOG.debug("=== Already-tried pair keys ({}) ===", alreadyTriedKeys.size());
        for (String key : alreadyTriedKeys) {
            String[] parts = key.split("::", 2);
            LOG.debug("  Already tried — SAST: {} | DAST: {}",
                parts.length == 2 ? parts[0] : key,
                parts.length == 2 ? parts[1] : "?");
        }

        LOG.debug("=== Mixed bucket SAST/DAST instance IDs ===");
        for (CategoryBucket bucket : buckets) {
            LOG.debug("  Category: {}", bucket.getCategory());
            for (Vulnerability sast : bucket.getSastFindings()) {
                LOG.debug("    SAST instanceId: {}", sast.getInstanceID());
            }
            for (DastIssue dast : bucket.getDastFindings()) {
                LOG.debug("    DAST issueId:    {}", dast.getId());
            }
        }

        if (alreadyTriedKeys.isEmpty()) {
            return buckets.stream().mapToInt(CategoryBucket::getSastCount).sum();
        }

        int count = 0;
        for (CategoryBucket bucket : buckets) {
            for (Vulnerability sast : bucket.getSastFindings()) {
                boolean hasNewPairing = bucket.getDastFindings().stream()
                    .anyMatch(dast -> !alreadyTriedKeys.contains(
                        sast.getInstanceID() + "::" + dast.getId()));
                LOG.debug("  SAST {} hasNewPairing={}", sast.getInstanceID(), hasNewPairing);
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
