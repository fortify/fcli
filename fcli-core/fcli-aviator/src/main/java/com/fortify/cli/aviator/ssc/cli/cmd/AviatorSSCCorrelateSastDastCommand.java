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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.session.user.cli.mixin.AviatorUserSessionDescriptorSupplier;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.dast.WebInspectParser;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.CorrelatedPair;
import com.fortify.cli.aviator.grpc.CorrelationStreamConfig;
import com.fortify.cli.aviator.grpc.CorrelationStreamProcessor;
import com.fortify.cli.aviator.ssc.helper.CategoryBucket;
import com.fortify.cli.aviator.ssc.helper.CategoryGrouper;
import com.fortify.cli.aviator.ssc.helper.ExternalFindingsInjector;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
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

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelateSastDastCommand.class);
    private String actionResult = "CORRELATED";

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var sessionDescriptor = sessionDescriptorSupplier.getSessionDescriptor();

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            SSCAppVersionDescriptor av = appVersionResolver.getAppVersionDescriptor(unirest);

            // Step 1: Download SAST and DAST FPRs from SSC
            SSCArtifactDescriptor adSast = getLatestSASTArtifact(unirest, av.getVersionId());
            Path downloadedSASTFprPath = downloadArtifactFpr(unirest, adSast, logger, progressWriter);

            SSCArtifactDescriptor adDast = getLatestDASTArtifact(unirest, av.getVersionId());
            Path downloadedDASTFprPath = downloadArtifactFpr(unirest, adDast, logger, progressWriter);

            validateDownloadedFpr(downloadedSASTFprPath, "SAST");
            validateDownloadedFpr(downloadedDASTFprPath, "DAST");

            // Step 2: Parse both FPRs
            ParseResult sastResult = parseSastFpr(downloadedSASTFprPath);
            ParseResult dastResult = parseDastFpr(downloadedDASTFprPath);

            // Step 3: Filter to unsuppressed issues only
            List<Vulnerability> unsuppressedSast = sastResult.vulnerabilities.stream()
                .filter(v -> !isVulnerabilitySuppressed(v, sastResult.auditIssueMap))
                .collect(Collectors.toList());
            List<DastIssue> unsuppressedDast = dastResult.dastIssues.stream()
                .filter(d -> !d.isSuppressed())
                .collect(Collectors.toList());

            LOG.info("Total SAST issues {}", sastResult.vulnerabilities.size());
            LOG.info("Total DAST issues {}", dastResult.dastIssues.size());

            LOG.info("Total Unsupressed SAST issues {}", unsuppressedSast.size());
            LOG.info("Total Unsupressed DAST issues {}", unsuppressedDast.size());
            // Step 4: Group by category
            CategoryGrouper grouper = new CategoryGrouper();
            grouper.groupFindings(unsuppressedSast, unsuppressedDast);
            grouper.printStatistics();
            List<CategoryBucket> mixedBuckets = grouper.getMixedBuckets();

            // Step 5: gRPC correlation (if mixed buckets exist)
            int submitted = mixedBuckets.stream().mapToInt(CategoryBucket::getSastCount).sum();
            /*List<CorrelatedPair> newPairs = new ArrayList<>();
            if (!mixedBuckets.isEmpty()) {
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

                try (AviatorGrpcClient grpcClient = AviatorGrpcClientHelper.createClient(aviatorUrl, logger, 30)) {
                    newPairs = performCorrelation(grpcClient, config, bucketData, sastResult.scanGuid, logger);
                }

                if (newPairs.isEmpty()) {
                    actionResult = "SKIPPED";
                } else if (newPairs.size() < submitted) {
                    actionResult = "PARTIALLY_CORRELATED";
                } else {
                    actionResult = "CORRELATED";
                }
            } else {
                actionResult = "SKIPPED";
                logger.info("No mixed categories found — skipping correlation.");
            }*/

            // Step 6: Inject <ExternalFindings> into DAST FPR and upload

            //Testing the upload flow
            String SAST_INSTANCE_1 = "00403DBC3662FEBAD561B1A578AE7556";
            String SAST_INSTANCE_2 = "00411ED275CA1DCF328136A99613E95E";
            String SAST_INSTANCE_3 = "0080AE7911F7A5D3A8BDEFD0DD046FB2";
            String DAST_ISSUE_1 = "b7391a4f-deb4-664d-3e23-ca2cc43a7527";
            String SAST_SCAN_GUID = "62cd94b2-523a-409e-86eb-9b55a0421380";
            List<CorrelatedPair> newPairs = List.of(
                new CorrelatedPair(SAST_INSTANCE_1, DAST_ISSUE_1, SAST_SCAN_GUID, "HIGH", "Primary match"),
                new CorrelatedPair(SAST_INSTANCE_2, DAST_ISSUE_1, SAST_SCAN_GUID, "MEDIUM", "Secondary match"),
                new CorrelatedPair(SAST_INSTANCE_3, DAST_ISSUE_1, SAST_SCAN_GUID, "LOW", "Tertiary match")
            );


            String uploadedArtifactId = null;
            if (!newPairs.isEmpty()) {
                ExternalFindingsInjector injector = new ExternalFindingsInjector();
                injector.injectAndRepackage(downloadedDASTFprPath, newPairs);
                try {
                    Files.copy(downloadedDASTFprPath, Path.of("C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-dast.fpr"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    LOG.info("Enriched DAST FPR saved to: C:/Users/nmeshram/Documents/TestSastDastCorrelation/enriched-dast.fpr");
                } catch (java.io.IOException e) {
                    LOG.warn("Failed to save enriched DAST FPR locally: {}", e.getMessage());
                }

                //First we have to delete the existing DAST FPR then upload the enriched DAST FPR. Only through this process
                // correlation data is available on the SSC



                logger.progress("Uploading correlated DAST FPR to SSC...");
                SSCFileTransferHelper.htmlUpload(
                    unirest,
                    SSCUrls.UPLOAD_RESULT_FILE(av.getVersionId()),
                    downloadedDASTFprPath.toFile(),
                    SSCFileTransferHelper.ISSCAddUploadTokenFunction.ROUTEPARAM_UPLOADTOKEN,
                    String.class,
                    progressWriter
                );
                uploadedArtifactId = adDast.getId();
            }

            // Step 7: Build output
            return buildOutputJson(av, uploadedArtifactId, submitted, newPairs);
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
            return processor.processCorrelation(config, bucketData, scanGuid)
                .get(grpcClient.getDefaultTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new FcliSimpleException("Correlation stream failed: " + e.getMessage(), e);
        }
    }

    private ObjectNode buildOutputJson(SSCAppVersionDescriptor av,
                                        String artifactId,
                                        int submitted,
                                        List<CorrelatedPair> newPairs) {
        int correlated = newPairs.size();
        int skipped = submitted - correlated;

        ObjectNode result = objectMapper.createObjectNode();
        result.put("id", av.getVersionId());
        result.put("applicationName", av.getApplicationName());
        result.put("versionName", av.getVersionName());
        if (artifactId != null) {
            result.put("artifactId", artifactId);
        } else {
            result.putNull("artifactId");
        }
        result.put("__action__", actionResult);

        ObjectNode operation = result.putObject("operation");
        ObjectNode correlate = operation.putObject("correlate");

        if (submitted > 0) {
            String message = String.format("%d SAST findings submitted, %d correlated pairs confirmed",
                    submitted, correlated);
            correlate.put("message", message);
            correlate.put("submitted", submitted);
            correlate.put("skipped", skipped);
        } else {
            correlate.putNull("message");
            correlate.putNull("submitted");
            correlate.putNull("skipped");
        }
        correlate.put("correlated", correlated);

        return result;
    }

    private boolean isVulnerabilitySuppressed(Vulnerability vuln, Map<String, AuditIssue> auditIssueMap) {
        if (auditIssueMap == null || vuln.getInstanceID() == null) {
            return false;
        }
        AuditIssue auditIssue = auditIssueMap.get(vuln.getInstanceID());
        return auditIssue != null && auditIssue.isSuppressed();
    }

    @Override
    public String getActionCommandResult() {
        return actionResult;
    }

    private void validateDownloadedFpr(Path fprPath, String label) {
        LOG.debug("Validate Download FPR {}", label);
        if (fprPath == null) {
            throw new FcliSimpleException(label + " FPR path is null; download may have failed");
        }
        if (!Files.exists(fprPath)) {
            throw new FcliSimpleException(label + " FPR file does not exist: " + fprPath);
        }
        if (!Files.isRegularFile(fprPath)) {
            throw new FcliSimpleException(label + " FPR path is not a regular file: " + fprPath);
        }
    }

    @SneakyThrows
    private Path downloadArtifactFpr(UnirestInstance unirest, SSCArtifactDescriptor ad,
                                      AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        Path fprPath = Files.createTempFile("aviator_" + ad.getId() + "_", ".fpr");
        logger.progress("Status: Downloading FPR from SSC (artifact id=" + ad.getId() + ")");
        SSCFileTransferHelper.download(
            unirest,
            SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
            fprPath.toFile(),
            SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
            progressWriter);
        return fprPath;
    }

    private static class ParseResult {
        List<Vulnerability> vulnerabilities = new ArrayList<>();
        List<DastIssue> dastIssues = new ArrayList<>();
        Map<String, AuditIssue> auditIssueMap;
        String buildId;
        String scanGuid;
    }

    private ParseResult parseSastFpr(Path sastfpr) {
        LOG.debug("Parsing SAST FPR ");
        try (FprHandle fprHandle = new FprHandle(sastfpr)) {
            fprHandle.validate();
            LOG.debug("Validation FPR handle done ");
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            StreamingFVDLProcessor fvdlProcessor = new StreamingFVDLProcessor(fprHandle);
            FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);

            ParseResult result = new ParseResult();
            result.vulnerabilities = vulnerabilities;
            result.auditIssueMap = auditIssueMap;
            if (!vulnerabilities.isEmpty()) {
                result.scanGuid = vulnerabilities.get(0).getUuid();
                result.buildId = vulnerabilities.get(0).getBuildId();
            }
            return result;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to parse SAST FPR: " + e.getMessage(), e);
        }
    }

    private ParseResult parseDastFpr(Path dastfpr) {
        try (FprHandle fprHandle = new FprHandle(dastfpr)) {
            if (!Files.exists(fprHandle.getPath("/webinspect.xml"))) {
                throw new FcliSimpleException("DAST FPR does not contain webinspect.xml");
            }
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            WebInspectParser parser = new WebInspectParser(fprHandle);
            List<DastIssue> issues = parser.parse();

            StreamingWebInspectParser streamingParser = new StreamingWebInspectParser(fprHandle);
            List<DastIssue> streamingDastIssues = streamingParser.parse();

            compareParserResults(issues, streamingDastIssues);

            for (DastIssue issue : streamingDastIssues) {
                if (issue.getId() != null && auditIssueMap.containsKey(issue.getId())) {
                    AuditIssue auditIssue = auditIssueMap.get(issue.getId());
                    issue.setSuppressed(auditIssue.isSuppressed());
                }
            }

            ParseResult result = new ParseResult();
            result.dastIssues = streamingDastIssues;
            result.auditIssueMap = auditIssueMap;
            return result;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to parse DAST FPR: " + e.getMessage(), e);
        }
    }

    private void compareParserResults(List<DastIssue> domIssues, List<DastIssue> staxIssues) {
        LOG.info("=== Parser Comparison: DOM vs Streaming WebInspect Parser ===");

        if (domIssues.size() != staxIssues.size()) {
            LOG.warn("Issue count MISMATCH: DOM={} vs Streaming={}", domIssues.size(), staxIssues.size());
        } else {
            LOG.info("Issue count matches: {}", domIssues.size());
        }

        int limit = Math.min(domIssues.size(), staxIssues.size());
        int mismatchCount = 0;

        for (int i = 0; i < limit; i++) {
            DastIssue dom = domIssues.get(i);
            DastIssue stax = staxIssues.get(i);
            List<String> diffs = compareIssueFields(dom, stax);

            if (!diffs.isEmpty()) {
                mismatchCount++;
                LOG.warn("Issue[{}] id='{}' has {} difference(s):", i, dom.getId(), diffs.size());
                diffs.forEach(d -> LOG.warn("  - {}", d));
            }
        }

        if (domIssues.size() > limit) {
            LOG.warn("{} extra issue(s) only in DOM parser (indices {} to {})",
                    domIssues.size() - limit, limit, domIssues.size() - 1);
        }
        if (staxIssues.size() > limit) {
            LOG.warn("{} extra issue(s) only in Streaming parser (indices {} to {})",
                    staxIssues.size() - limit, limit, staxIssues.size() - 1);
        }

        if (mismatchCount == 0 && domIssues.size() == staxIssues.size()) {
            LOG.info("Parser comparison PASSED: all {} issues are identical", domIssues.size());
        } else {
            LOG.warn("Parser comparison FAILED: {}/{} common issues have differences",
                    mismatchCount, limit);
        }

        LOG.info("=== End Parser Comparison ===");
    }

    private List<String> compareIssueFields(DastIssue dom, DastIssue stax) {
        List<String> diffs = new ArrayList<>();

        compareField(diffs, "id", dom.getId(), stax.getId());
        compareField(diffs, "checkTypeId", dom.getCheckTypeId(), stax.getCheckTypeId());
        compareField(diffs, "engineType", dom.getEngineType(), stax.getEngineType());
        compareField(diffs, "vulnerabilityId", dom.getVulnerabilityId(), stax.getVulnerabilityId());
        if (dom.getSeverity() != stax.getSeverity()) {
            diffs.add(String.format("severity: DOM=%d vs Streaming=%d",
                    dom.getSeverity(), stax.getSeverity()));
        }
        compareField(diffs, "name", dom.getName(), stax.getName());
        compareField(diffs, "category", dom.getCategory(), stax.getCategory());
        compareField(diffs, "cweId", dom.getCweId(), stax.getCweId());
        compareField(diffs, "sessionUrl", dom.getSessionUrl(), stax.getSessionUrl());
        compareField(diffs, "summary", dom.getSummary(), stax.getSummary());

        if (!Objects.equals(dom.getReproStepUrls(), stax.getReproStepUrls())) {
            diffs.add(String.format("reproStepUrls: DOM=%s vs Streaming=%s",
                    dom.getReproStepUrls(), stax.getReproStepUrls()));
        }

        return diffs;
    }

    private void compareField(List<String> diffs, String fieldName, String domVal, String staxVal) {
        if (!Objects.equals(domVal, staxVal)) {
            diffs.add(String.format("%s: DOM='%s' vs Streaming='%s'", fieldName, domVal, staxVal));
        }
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
