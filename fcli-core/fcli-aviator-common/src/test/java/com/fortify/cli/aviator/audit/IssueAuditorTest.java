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
package com.fortify.cli.aviator.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.cli.aviator._common.exception.AviatorQuotaFilterException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.grpc.SastStreamOutcome;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.ResourceUtil;
import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.filter.Filter;
import com.fortify.cli.common.fpr.filter.FilterSelection;
import com.fortify.cli.common.fpr.filter.FilterSet;
import com.fortify.cli.common.fpr.filter.FilterTemplate;
import com.fortify.cli.common.fpr.model.AuditIssue;
import com.fortify.cli.common.fpr.model.FPRInfo;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.SourceDecoders;


class IssueAuditorTest {

    private static final String TEST_ISSUE_ID = "PROCESSED_ISSUE";
    private static final IAviatorLogger NO_OP_LOGGER = new IAviatorLogger() {
        @Override public void progress(String format, Object... args) {}
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) {}
        @Override public void error(String format, Object... args) {}
    };

    private Path tempFprFile;
    private FprHandle fprHandle;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> progress = new ArrayList<>();
    private final IAviatorLogger outcomeLogger = new IAviatorLogger() {
        @Override public void progress(String format, Object... args) { progress.add(String.format(format, args)); }
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) { warnings.add(String.format(format, args)); }
        @Override public void error(String format, Object... args) {}
    };

    @BeforeEach
    void setup() throws IOException {
        // 1. Create a temporary dummy FPR file (ZIP format)
        tempFprFile = Files.createTempFile("test_aviator", ".fpr");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {

            // A. Add a minimal audit.fvdl
            ZipEntry entry = new ZipEntry("audit.fvdl");
            zos.putNextEntry(entry);
            String minimalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FVDL><UUID>test-uuid</UUID><Build><BuildID>test-build</BuildID></Build></FVDL>";
            zos.write(minimalXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // B. Add src-archive/index.xml to suppress the warning
            ZipEntry indexEntry = new ZipEntry("src-archive/index.xml");
            zos.putNextEntry(indexEntry);
            String indexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><index></index>";
            zos.write(indexXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        fprHandle = new FprHandle(tempFprFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (tempFprFile != null) {
            Files.deleteIfExists(tempFprFile);
        }
    }

    @Test
    void retainsPartialResultsForFprFinalizationAfterTerminalFailure() throws Exception {
        IssueAuditor auditor = createIssueAuditor(false, false, Map.of());
        var response = AuditResponse.builder().issueId(TEST_ISSUE_ID).status("SUCCESS").tier("GOLD")
                .submittedToAviator(true)
                .auditResult(AuditResult.builder().tagValue("Exploitable").comment("Audit result").build()).build();
        var failure = new SastStreamOutcome.Failed(Map.of(TEST_ISSUE_ID, response),
                new AviatorTechnicalException("Injected stream failure"));
        Map<String, AuditResponse> responses = new HashMap<>();

        var outcome = auditor.collectAuditOutcome(CompletableFuture.completedFuture(failure), responses, 2);

        assertSame(response, responses.get(TEST_ISSUE_ID));
        assertSame(responses, outcome.getAuditResponses());
        assertEquals(2, outcome.getTotalIssuesToAudit());
        assertEquals("PARTIALLY_AUDITED", AuditFPR.determineAuditStatus(1, 1, 2, responses.size()));
        assertEquals(List.of("Audit stream failed. Retaining 1 received results; other results are unavailable."), warnings);
        assertTrue(progress.isEmpty(), "A failed stream must not announce normal completion");

        var parsed = AviatorSastFprParser.parse(fprHandle, SourceDecoders.defaults());
        var mapping = ResourceUtil.loadYamlResource("default_tag_mapping.yaml", TagMappingConfig.class).resolveForSast();
        var updatedFile = parsed.auditProcessor.updateAndSaveAuditAndRemediationsXml(
                outcome.getAuditResponses(), mapping, Map.of(), parsed.fprInfo);

        assertEquals(tempFprFile, updatedFile.toPath());
        String auditXml = Files.readString(fprHandle.getPath("/audit.xml"));
        assertTrue(auditXml.contains("instanceId=\"" + TEST_ISSUE_ID + "\""));
        assertTrue(auditXml.contains("Audit result"));
        fprHandle.close();
        fprHandle = new FprHandle(tempFprFile);
        assertTrue(Files.readString(fprHandle.getPath("/audit.xml")).contains("Audit result"),
                "The received audit must persist after closing and reopening the FPR");
    }

    @Test
    void keepsTerminalFailureWhenNoPartialResultsExist() {
        IssueAuditor auditor = createIssueAuditor(false, false, Map.of());
        var failure = new AviatorTechnicalException("Injected stream failure");
        Map<String, AuditResponse> responses = new HashMap<>();

        var thrown = assertThrows(AviatorTechnicalException.class,
                () -> auditor.collectAuditOutcome(CompletableFuture.completedFuture(
                        new SastStreamOutcome.Failed(Map.of(), failure)), responses, 2));

        assertSame(failure, thrown);
        assertTrue(responses.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Exploitable"})
    void terminalFailureWithoutUsableSuccessIsNotFinalized(String tagValue) {
        IssueAuditor auditor = createIssueAuditor(false, false, Map.of());
        var failure = new AviatorTechnicalException("Injected stream failure");
        var response = AuditResponse.builder().issueId(TEST_ISSUE_ID)
                .status("Exploitable".equals(tagValue) ? "FAILED" : "SUCCESS")
                .auditResult(AuditResult.builder().tagValue(tagValue).build()).build();
        var result = new SastStreamOutcome.Failed(Map.of(TEST_ISSUE_ID, response), failure);
        Map<String, AuditResponse> responses = new HashMap<>();

        assertSame(failure, assertThrows(AviatorTechnicalException.class,
                () -> auditor.collectAuditOutcome(CompletableFuture.completedFuture(result), responses, 2)));
        assertTrue(responses.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void terminalFailureWithMissingAuditResultIsNotFinalized() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var response = AuditResponse.builder().issueId(TEST_ISSUE_ID).status("SUCCESS").build();
        var failure = new AviatorTechnicalException("Injected stream failure");
        var result = new SastStreamOutcome.Failed(Map.of(TEST_ISSUE_ID, response), failure);

        assertSame(failure, assertThrows(AviatorTechnicalException.class,
                () -> auditor.collectAuditOutcome(CompletableFuture.completedFuture(result), new HashMap<>(), 2)));
    }

    @Test
    void completedStreamRetainsIssueLevelFailureWithoutPartialWarning() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var response = AuditResponse.builder().issueId(TEST_ISSUE_ID).status("FAILED").build();
        var result = new SastStreamOutcome.Completed(Map.of(TEST_ISSUE_ID, response));

        var outcome = auditor.collectAuditOutcome(CompletableFuture.completedFuture(result), new HashMap<>(), 1);

        assertSame(response, outcome.getAuditResponses().get(TEST_ISSUE_ID));
        assertEquals(1, outcome.getTotalIssuesToAudit());
        assertTrue(warnings.isEmpty());
        assertEquals(List.of("Audit stream completed"), progress);
    }

    @Test
    void quotaFilteringStillReportsZeroAuditableIssues() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var failure = new AviatorQuotaFilterException("No quota");

        var outcome = auditor.collectAuditOutcome(CompletableFuture.failedFuture(failure), new HashMap<>(), 2);

        assertEquals(0, outcome.getTotalIssuesToAudit());
        assertTrue(outcome.getAuditResponses().isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void initializationFailureKeepsExistingDomainException() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var failure = new AviatorSimpleException("Audit initialization timed out");

        assertSame(failure, assertThrows(AviatorSimpleException.class,
                () -> auditor.collectAuditOutcome(CompletableFuture.completedFuture(
                        new SastStreamOutcome.Failed(Map.of(), failure)), new HashMap<>(), 2)));
    }

    @Test
    void unexpectedExceptionalFutureIsNotFinalized() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var failure = new IllegalStateException("Injected implementation failure");

        var thrown = assertThrows(AviatorTechnicalException.class,
                () -> auditor.collectAuditOutcome(CompletableFuture.failedFuture(failure), new HashMap<>(), 2));

        assertSame(failure, thrown.getCause());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void interruptedWaitRestoresInterruptAndDoesNotFinalize() {
        var auditor = createIssueAuditor(false, false, Map.of());
        Thread.currentThread().interrupt();
        try {
            var thrown = assertThrows(AviatorTechnicalException.class,
                    () -> auditor.collectAuditOutcome(new CompletableFuture<>(), new HashMap<>(), 2));
            assertEquals("Audit interrupted", thrown.getMessage());
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(warnings.isEmpty());
        } finally {
            Thread.interrupted();
        }
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test
    void outerTimeoutRemainsExceptionalWithoutPartialFinalization() {
        var auditor = createIssueAuditor(false, false, Map.of());
        var timeout = new TimeoutException("Injected outer timeout");
        var future = new CompletableFuture<SastStreamOutcome>() {
            @Override
            public SastStreamOutcome get(long duration, TimeUnit unit) throws TimeoutException {
                assertEquals(500, duration);
                assertEquals(TimeUnit.MINUTES, unit);
                throw timeout;
            }
        };

        var thrown = assertThrows(AviatorTechnicalException.class,
                () -> auditor.collectAuditOutcome(future, new HashMap<>(), 2));

        assertSame(timeout, thrown.getCause());
        assertEquals("Audit timed out after 500 minutes", thrown.getMessage());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void testFprInfoMissingBuildIdDefaultsToEmptyString() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
            fprHandle = null;
        }
        if (tempFprFile != null) {
            Files.deleteIfExists(tempFprFile);
        }

        tempFprFile = Files.createTempFile("test_aviator_missing_build", ".fpr");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(tempFprFile))) {
            ZipEntry entry = new ZipEntry("audit.fvdl");
            zos.putNextEntry(entry);
            String minimalXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FVDL><UUID>test-uuid</UUID><Build></Build></FVDL>";
            zos.write(minimalXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            ZipEntry indexEntry = new ZipEntry("src-archive/index.xml");
            zos.putNextEntry(indexEntry);
            String indexXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><index></index>";
            zos.write(indexXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        fprHandle = new FprHandle(tempFprFile);
        FPRInfo fprInfo = new FPRInfo(fprHandle);

        assertEquals("", fprInfo.getBuildId());
    }

    @Test
    void skipsPreviouslyProcessedAviatorIssueWithoutForceReaudit() throws Exception {
        IssueAuditor auditor = createIssueAuditor(false, false,
                Map.of(Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditIncludesProcessedAviatorIssueAndIgnoresAviatorOutcomeTag() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID, Constants.EXPLOITABLE));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void forceReauditIncludesProcessedAviatorIssueWithAnalysisTag() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void forceReauditStillSkipsSuppressedIssue() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, true,
                Map.of(Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditStillSkipsHumanTriagedIssue() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.FOD_TAG_ID, Constants.EXPLOITABLE));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditIncludesIssueWithPendingReviewState() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.FOD_TAG_ID, "Pending Review",
                Constants.AUDITOR_STATUS_TAG_ID, Constants.PENDING_REVIEW));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void treatsAllPendingAnalysisValuesAsUnaudited() throws Exception {
        for (String pendingValue : List.of("Pending Review", "Not Set", "Pending Review/Not Set", " pending review ")) {
            IssueAuditor auditor = createIssueAuditor(false, false, Map.of(Constants.ANALYSIS_TAG_ID, pendingValue));

            assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor), pendingValue);
        }
    }

    @Test
    void forceReauditStillSkipsIssueWithManualAuditorStatus() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.AUDITOR_STATUS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.AUDITOR_STATUS_TAG_ID, "analyst.user"));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditIncludesAviatorWrittenAuditorStatus() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.AUDITOR_STATUS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.AUDITOR_STATUS_TAG_ID, Constants.USER_NAME));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void forceReauditSkipsHumanAnalysisOverrideAfterAviator() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.ANALYSIS_TAG_ID, "analyst.user"));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditIncludesLegacyFortifyAviatorUsername() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false, Map.of(
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.ANALYSIS_TAG_ID, Constants.USER_NAME_LEGACY_FORTIFY_AVIATOR));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void forceReauditIncludesLegacyAnalysisTagWrittenByAviatorWithoutStatusTag() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false,
                Map.of(Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.ANALYSIS_TAG_ID, Constants.USER_NAME_LEGACY_FORTIFY_AVIATOR));

        assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
    }

    @Test
    void forceReauditSkipsLegacyAnalysisTagWrittenByHumanWithoutStatusTag() throws Exception {
        IssueAuditor auditor = createIssueAuditor(true, false,
                Map.of(Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE),
                Map.of(Constants.ANALYSIS_TAG_ID, "analyst.user"));

        assertTrue(prepareIssueIds(auditor).isEmpty());
    }

    @Test
    void forceReauditIncludesMappedTagWrittenByAviatorWithoutStatusTag() throws Exception {
        Path mappingFile = writeMappedTagFile();
        try {
            IssueAuditor auditor = createIssueAuditor(true, false,
                    Map.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", Constants.EXPLOITABLE),
                    Map.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", Constants.USER_NAME),
                    mappingFile.toString());
            assertEquals(List.of(TEST_ISSUE_ID), prepareIssueIds(auditor));
        } finally {
            Files.deleteIfExists(mappingFile);
        }
    }

    @Test
    void forceReauditSkipsHumanWriterOnMappedTag() throws Exception {
        Path mappingFile = writeMappedTagFile();
        try {
            IssueAuditor auditor = createIssueAuditor(true, false,
                    Map.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", Constants.EXPLOITABLE),
                    Map.of("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "analyst.user"),
                    mappingFile.toString());
            assertTrue(prepareIssueIds(auditor).isEmpty());
        } finally {
            Files.deleteIfExists(mappingFile);
        }
    }

    private Path writeMappedTagFile() throws IOException {
        Path mappingFile = Files.createTempFile("tag-mapping", ".yaml");
        Files.writeString(mappingFile, "tag_id: \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\"\n");
        return mappingFile;
    }

    private IssueAuditor createIssueAuditor(boolean forceReaudit, boolean suppressed, Map<String, String> tags) {
        return createIssueAuditor(forceReaudit, suppressed, tags, Map.of(), null);
    }

    private IssueAuditor createIssueAuditor(boolean forceReaudit, boolean suppressed, Map<String, String> tags,
            Map<String, String> lastTagUsernames) {
        return createIssueAuditor(forceReaudit, suppressed, tags, lastTagUsernames, null);
    }

    private IssueAuditor createIssueAuditor(boolean forceReaudit, boolean suppressed, Map<String, String> tags,
            Map<String, String> lastTagUsernames, String tagMappingPath) {
        FPRInfo fprInfo = new FPRInfo(fprHandle);
        FilterTemplate filterTemplate = new FilterTemplate();
        filterTemplate.setTagDefinitions(new ArrayList<>());
        fprInfo.setFilterTemplate(filterTemplate);

        Vulnerability vulnerability = new Vulnerability();
        vulnerability.setInstanceID(TEST_ISSUE_ID);
        AuditIssue auditIssue = AuditIssue.builder()
                .instanceId(TEST_ISSUE_ID)
                .suppressed(suppressed)
                .tags(new HashMap<>(tags))
                .lastTagUsernames(new HashMap<>(lastTagUsernames))
                .build();

        return IssueAuditor.builder()
                .vulnerabilities(List.of(vulnerability))
                .auditIssueMap(Map.of(TEST_ISSUE_ID, auditIssue))
                .fprInfo(fprInfo)
                .filterSelection(new FilterSelection(null, null))
                .sourceLanguageResolver(new SourceLanguageResolver(new FVDLMetadata()))
                .options(auditOptions(forceReaudit, outcomeLogger, tagMappingPath))
                .build();
    }

    private AuditFprOptions auditOptions(boolean forceReaudit, IAviatorLogger logger) {
        return auditOptions(forceReaudit, logger, null);
    }

    private AuditFprOptions auditOptions(boolean forceReaudit, IAviatorLogger logger, String tagMappingPath) {
        return AuditFprOptions.builder()
                .fprHandle(fprHandle)
                .logger(logger)
                .sscAppName("TestApp")
                .sscAppVersion("1.0")
                .tagMappingPath(tagMappingPath)
                .forceReaudit(forceReaudit)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> prepareIssueIds(IssueAuditor auditor) throws Exception {
        Method prepareMethod = IssueAuditor.class.getDeclaredMethod("prepareAndFilterPrompts");
        prepareMethod.setAccessible(true);
        ConcurrentLinkedDeque<UserPrompt> prompts = (ConcurrentLinkedDeque<UserPrompt>) prepareMethod.invoke(auditor);
        return prompts.stream()
                .map(prompt -> prompt.getIssueData().getInstanceID())
                .collect(Collectors.toList());
    }

    @Test
    void testFilterVulnerabilities_LegacySyntaxWithSpaces() throws Exception {

        // Manual Logger Stub (No-op)
        IAviatorLogger dummyLogger = new IAviatorLogger() {
            @Override public void progress(String format, Object... args) {}
            @Override public void info(String format, Object... args) {}
            @Override public void warn(String format, Object... args) {}
            @Override public void error(String format, Object... args) {}
        };

        FPRInfo fprInfo = new FPRInfo(fprHandle);

        FilterTemplate dummyTemplate = new FilterTemplate();
        dummyTemplate.setTagDefinitions(new ArrayList<>());
        // Note: FPRInfo.setFilterTemplate() exists in the provided code
        fprInfo.setFilterTemplate(dummyTemplate);

        // --- 2. SETUP FILTER SET WITH LEGACY SYNTAX ---
        // The "Buggy" Filter String (Legacy syntax: Space in range AND between terms)
        String legacyQuery = "impact:![2.5, 5.0] [Analysis Type]:!SONATYPE";

        Filter filter = new Filter();
        filter.setAction("hide");
        filter.setQuery(legacyQuery);

        FilterSet filterSet = new FilterSet();
        filterSet.setTitle("Regression Test Set");
        filterSet.setFilters(Collections.singletonList(filter));

        FilterSelection selection = new FilterSelection(filterSet, null);

        // Target Issue: High Impact (4.0), Null Analysis Type.
        // Result: Should NOT be hidden.
        Vulnerability targetVuln = new Vulnerability();
        targetVuln.setInstanceID("TARGET_ISSUE");
        targetVuln.setImpact(4.0);

        // Noise Issue: Low Impact (1.0).
        // Result: Should be hidden.
        Vulnerability hiddenVuln = new Vulnerability();
        hiddenVuln.setInstanceID("HIDDEN_ISSUE");
        hiddenVuln.setImpact(1.0);

        List<Vulnerability> inputList = Arrays.asList(targetVuln, hiddenVuln);

        IssueAuditor auditor = IssueAuditor.builder()
                .vulnerabilities(inputList)
                .auditIssueMap(new HashMap<>())
                .fprInfo(fprInfo)
                .filterSelection(selection)
                .sourceLanguageResolver(new SourceLanguageResolver(new FVDLMetadata()))
                .options(auditOptions(false, dummyLogger))
                .build();

        Method filterMethod = IssueAuditor.class.getDeclaredMethod("filterVulnerabilities", List.class, FilterSet.class);
        filterMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Vulnerability> results = (List<Vulnerability>) filterMethod.invoke(auditor, inputList, filterSet);

        List<String> remainingIds = results.stream()
                .map(Vulnerability::getInstanceID)
                .collect(Collectors.toList());

        assertEquals(1, remainingIds.size(), "Should verify that exactly one issue remains");
        assertTrue(remainingIds.contains("TARGET_ISSUE"),
            "Regression Failed: IssueAuditor hid the target issue. It likely used the Modern parser on a Legacy query string.");
    }
}
