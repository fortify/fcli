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
import java.util.List;
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
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateDownloadHelper;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateFprParser.ParseResult;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCCorrelateHelper;
import com.fortify.cli.aviator.ssc.helper.CategoryBucket;
import com.fortify.cli.aviator.ssc.helper.CategoryGrouper;
import com.fortify.cli.aviator.ssc.helper.DastFprCorrelationEnricher;
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

            LOG.info("Total SAST issues {}", sastResult.vulnerabilities.size());
            LOG.info("Total DAST issues {}", dastResult.dastIssues.size());
            LOG.info("Total Unsupressed SAST issues {}", unsuppressedSast.size());
            LOG.info("Total Unsupressed DAST issues {}", unsuppressedDast.size());

            logger.progress("Status: Found %d SAST and %d DAST unsuppressed issues to correlate",
                unsuppressedSast.size(), unsuppressedDast.size());

            // Step 4: Group by category
            logger.progress("Status: Grouping findings by vulnerability category...");
            CategoryGrouper grouper = new CategoryGrouper();
            grouper.groupFindings(unsuppressedSast, unsuppressedDast);
            grouper.printStatistics();
            List<CategoryBucket> mixedBuckets = grouper.getMixedBuckets();

            // Step 5: gRPC correlation (if mixed buckets exist)
            int submitted = mixedBuckets.stream().mapToInt(CategoryBucket::getSastCount).sum();
            List<CorrelatedPair> newPairs = new ArrayList<>();
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
                    newPairs = performCorrelation(grpcClient, config, bucketData, sastResult.scanGuid, logger);
                }

                logger.progress("Status: Correlation complete — %d of %d SAST findings confirmed as correlated",
                    newPairs.size(), submitted);

                if (newPairs.isEmpty()) {
                    actionResult = "SKIPPED";
                } else if (newPairs.size() < submitted) {
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

            // Step 7: Build output
            logger.progress("Status: Correlation process complete for %s:%s — result: %s",
                av.getApplicationName(), av.getVersionName(), actionResult);
            return AviatorSSCCorrelateHelper.buildOutputJson(av, uploadedArtifactId, submitted, newPairs, actionResult);
        }
    }

    private List<CorrelatedPair> performCorrelation(
            AviatorGrpcClient grpcClient,
            CorrelationStreamConfig config,
            List<CorrelationStreamProcessor.CorrelationBucketData> bucketData,
            String scanGuid,
            AviatorLoggerImpl logger) {
        try {
            var processor = new CorrelationStreamProcessor(
                grpcClient, logger,
                grpcClient.getCorrelationAsyncStub(),
                grpcClient.getPingScheduler(),
                grpcClient.getPingIntervalSeconds(),
                grpcClient.getDefaultTimeoutSeconds()
            );
            long timeoutSeconds = Math.max(grpcClient.getDefaultTimeoutSeconds(), 300);
            return processor.processCorrelation(config, bucketData, scanGuid)
                .get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new FcliSimpleException("Correlation stream timed out waiting for server responses", e);
        } catch (Exception e) {
            throw new FcliSimpleException("Correlation stream failed: " + e.getMessage(), e);
        }
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
