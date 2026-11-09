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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.AuditOutcome;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResult;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.audit.model.FilterSelection;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

/** Re-audit on code change: selection, baselining, authorship scope and persistence of the evidence fingerprint. */
class ReauditOnCodeChangeTest {
    private static final String AUDIT_NS = "xmlns://www.fortify.com/schema/audit";
    private static final String ISSUE_ID = "ISSUE-1";
    /** Reference-implementation fingerprint of a 60-line file whose lines read "line N", trace at line 30. */
    private static final String SOURCE_FINGERPRINT = "v1:0db51319abef8b1af41c51a2cc224bcb0609d2be3a198f9de9f7c64e9a76f243";
    private static final String STALE_FINGERPRINT = "v1:" + "0".repeat(64);
    private static final IAviatorLogger NO_OP_LOGGER = new IAviatorLogger() {
        @Override public void progress(String format, Object... args) {}
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) {}
        @Override public void error(String format, Object... args) {}
    };

    private record History(String tagId, String value, String user) {}

    private Path fprPath;
    private FprHandle fprHandle;
    private AuditProcessor auditProcessor;
    private Map<String, AuditIssue> auditIssueMap;

    @AfterEach
    void tearDown() throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
        }
        if (fprPath != null) {
            Files.deleteIfExists(fprPath);
        }
    }

    @Test
    void recordsBaselineForAviatorAuditedIssueWithoutFingerprintAndDoesNotReaudit() throws Exception {
        setUp(issueXml(ISSUE_ID, false, aviatorTags(), aviatorHistory()), "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "Main.java");

        assertTrue(prompts.isEmpty());
        assertEquals(1, auditProcessor.getBaselinedCount());
        save();
        Element issue = savedIssue(ISSUE_ID);
        assertEquals(SOURCE_FINGERPRINT, liveTagValue(issue, Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID));
        assertEquals("1", issue.getAttribute("revision"));
        assertTrue(historyUsers(issue, Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID).contains(Constants.USER_NAME));
    }

    @Test
    void doesNothingWhenStoredFingerprintMatches() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, SOURCE_FINGERPRINT);
        setUp(issueXml(ISSUE_ID, false, tags, aviatorHistory()), "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "Main.java");

        assertTrue(prompts.isEmpty());
        assertEquals(0, auditProcessor.getBaselinedCount());
        assertFalse(auditProcessor.isReaudit(ISSUE_ID));
    }

    @Test
    void queuesReauditWhenStoredFingerprintDiffers() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT);
        setUp(issueXml(ISSUE_ID, false, tags, aviatorHistory()), "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "Main.java");

        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0).isReaudit());
        assertTrue(auditProcessor.isReaudit(ISSUE_ID));
        assertEquals(0, auditProcessor.getBaselinedCount());
    }

    @Test
    void excludesIssueLastAuditedByAHumanUnlessAllIssuesIsSet() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE);
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT);
        List<History> history = new ArrayList<>(aviatorHistory());
        history.add(new History(Constants.ANALYSIS_TAG_ID, Constants.EXPLOITABLE, "alice"));
        setUp(issueXml(ISSUE_ID, false, tags, history), "Main.java");

        assertTrue(prepare(false, false, "Main.java").isEmpty());
        assertFalse(auditProcessor.isReaudit(ISSUE_ID));
        assertEquals(0, auditProcessor.getBaselinedCount());

        List<UserPrompt> withAllIssues = prepare(false, true, "Main.java");

        assertEquals(1, withAllIssues.size());
        assertTrue(withAllIssues.get(0).isReaudit());
    }

    @Test
    void includesIssueSuppressedByAviatorButNotOneSuppressedByAHuman() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT);

        List<History> byAviator = new ArrayList<>(aviatorHistory());
        byAviator.add(new History(Constants.SUPPRESSED_TAG_ID, "true", Constants.USER_NAME));
        setUp(issueXml(ISSUE_ID, true, tags, byAviator), "Main.java");
        assertEquals(1, prepare(false, false, "Main.java").size());

        List<History> byHuman = new ArrayList<>(aviatorHistory());
        byHuman.add(new History(Constants.SUPPRESSED_TAG_ID, "true", "alice"));
        setUp(issueXml(ISSUE_ID, true, tags, byHuman), "Main.java");
        assertTrue(prepare(false, false, "Main.java").isEmpty());

        setUp(issueXml(ISSUE_ID, true, tags, aviatorHistory()), "Main.java");
        assertTrue(prepare(false, false, "Main.java").isEmpty());
    }

    @Test
    void issueWithoutReadableSourceIsNeitherBaselinedNorReaudited() throws Exception {
        setUp(issueXml(ISSUE_ID, false, aviatorTags(), aviatorHistory()), "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "NotInArchive.java");

        assertTrue(prompts.isEmpty());
        assertEquals(0, auditProcessor.getBaselinedCount());
    }

    @Test
    void storesFingerprintAfterAFirstTimeAuditOfAnIssueNotYetInAuditXml() throws Exception {
        setUp(emptyAuditXml(), "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "Main.java");
        assertEquals(1, prompts.size());
        assertFalse(prompts.get(0).isReaudit());

        auditProcessor.updateAndSaveAuditAndRemediationsXml(Map.of(ISSUE_ID, successResponse(ISSUE_ID)),
                tagMapping(), Map.of(), new FPRInfo(fprHandle));

        assertEquals(SOURCE_FINGERPRINT, liveTagValue(savedIssue(ISSUE_ID), Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID));
    }

    @Test
    void storesFingerprintAfterAFirstTimeAuditOfAnIssueAlreadyInAuditXml() throws Exception {
        setUp(issueXml(ISSUE_ID, false, new LinkedHashMap<>(), List.of()), "Main.java");

        prepare(false, false, "Main.java");
        auditProcessor.updateAndSaveAuditAndRemediationsXml(Map.of(ISSUE_ID, successResponse(ISSUE_ID)),
                tagMapping(), Map.of(), new FPRInfo(fprHandle));

        assertEquals(SOURCE_FINGERPRINT, liveTagValue(savedIssue(ISSUE_ID), Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID));
    }

    @Test
    void reauditUpdatesTheLiveFingerprintEvenWhenAuditTrailPrecedesItInTheXml() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4">
                  <ns2:IssueList>
                    <ns2:Issue instanceId="%s" revision="3" suppressed="false">
                      <ns2:Tag id="%s"><ns2:Value>%s</ns2:Value></ns2:Tag>
                      <ns2:ClientAuditTrail>
                        <ns2:TagHistory><ns2:Tag id="%s"><ns2:Value>%s</ns2:Value></ns2:Tag><ns2:EditTime>2026-01-01T00:00:00.000+00:00</ns2:EditTime><ns2:Username>%s</ns2:Username></ns2:TagHistory>
                        <ns2:TagHistory><ns2:Tag id="%s"><ns2:Value>%s</ns2:Value></ns2:Tag><ns2:EditTime>2026-01-01T00:00:00.000+00:00</ns2:EditTime><ns2:Username>%s</ns2:Username></ns2:TagHistory>
                      </ns2:ClientAuditTrail>
                      <ns2:Tag id="%s"><ns2:Value>%s</ns2:Value></ns2:Tag>
                    </ns2:Issue>
                  </ns2:IssueList>
                </ns2:Audit>
                """.formatted(AUDIT_NS, ISSUE_ID,
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR,
                Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR, Constants.USER_NAME,
                Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT, Constants.USER_NAME,
                Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT);
        setUp(xml, "Main.java");

        List<UserPrompt> prompts = prepare(false, false, "Main.java");
        assertEquals(1, prompts.size());
        auditProcessor.updateAndSaveAuditAndRemediationsXml(Map.of(ISSUE_ID, successResponse(ISSUE_ID)),
                tagMapping(), Map.of(), new FPRInfo(fprHandle));

        Element issue = savedIssue(ISSUE_ID);
        assertEquals(SOURCE_FINGERPRINT, liveTagValue(issue, Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID));
        assertEquals("4", issue.getAttribute("revision"));
        assertTrue(comments(issue).stream().anyMatch(c -> c.contains("source code behind this issue changed")
                && c.contains("Previous verdict") && c.contains("New verdict: " + Constants.NOT_AN_ISSUE)));
    }

    @Test
    void failedReauditKeepsThePreviousFingerprintAndVerdict() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, STALE_FINGERPRINT);
        setUp(issueXml(ISSUE_ID, false, tags, aviatorHistory()), "Main.java");

        assertEquals(1, prepare(false, false, "Main.java").size());
        AuditResponse failed = AuditResponse.builder().issueId(ISSUE_ID).status("FAILED").statusMessage("backend error").build();
        auditProcessor.updateAndSaveAuditAndRemediationsXml(Map.of(ISSUE_ID, failed), tagMapping(), Map.of(),
                new FPRInfo(fprHandle));

        assertTrue(savedIssueIds().isEmpty());
    }

    @Test
    void baselineOnlyRunStillSavesTheFprAndReportsTheBaselines() throws Exception {
        setUp(issueXml(ISSUE_ID, false, aviatorTags(), aviatorHistory()), "Main.java");
        prepare(false, false, "Main.java");

        FPRAuditResult result = AuditFPR.finalizeFprAudit(new AuditOutcome(Map.of(), 0), Map.of(), auditProcessor,
                tagMapping(), Map.of(), new FPRInfo(fprHandle), new StreamingFVDLProcessor(fprHandle));

        assertEquals("SKIPPED", result.getStatus());
        assertNotNull(result.getUpdatedFile());
        assertEquals(1, result.getEvidenceBaselines());
        assertEquals(SOURCE_FINGERPRINT, liveTagValue(savedIssue(ISSUE_ID), Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID));
    }

    @Test
    void runWithNothingToDoStillSkipsWithoutSaving() throws Exception {
        Map<String, String> tags = aviatorTags();
        tags.put(Constants.AVIATOR_SOURCE_FINGERPRINT_TAG_ID, SOURCE_FINGERPRINT);
        setUp(issueXml(ISSUE_ID, false, tags, aviatorHistory()), "Main.java");
        prepare(false, false, "Main.java");

        FPRAuditResult result = AuditFPR.finalizeFprAudit(new AuditOutcome(Map.of(), 0), Map.of(), auditProcessor,
                tagMapping(), Map.of(), new FPRInfo(fprHandle), new StreamingFVDLProcessor(fprHandle));

        assertEquals("SKIPPED", result.getStatus());
        assertNull(result.getUpdatedFile());
        assertEquals(0, result.getEvidenceBaselines());
    }

    // ---------------------------------------------------------------- fixtures

    private void setUp(String auditXml, String sourceFile) throws Exception {
        if (fprHandle != null) {
            fprHandle.close();
            Files.deleteIfExists(fprPath);
        }
        fprPath = Files.createTempFile("reaudit-on-change", ".fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            put(zip, "audit.fvdl", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><FVDL><UUID>uuid-1</UUID><Build>"
                    + "<BuildID>build-1</BuildID><SourceBasePath>.</SourceBasePath><NumberFiles>1</NumberFiles>"
                    + "<ScanTime>1</ScanTime></Build></FVDL>");
            put(zip, "audit.xml", auditXml);
            put(zip, "src-archive/index.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><index><entry key=\""
                    + sourceFile + "\">src-archive/" + sourceFile + "</entry></index>");
            put(zip, "src-archive/" + sourceFile, numbered(60));
        }
        fprHandle = new FprHandle(fprPath);
        auditProcessor = new AuditProcessor(fprHandle);
        auditIssueMap = auditProcessor.processAuditXML();
    }

    private List<UserPrompt> prepare(boolean forceReaudit, boolean allIssues, String traceFile) throws Exception {
        FPRInfo fprInfo = new FPRInfo(fprHandle);
        FilterTemplate filterTemplate = new FilterTemplate();
        filterTemplate.setTagDefinitions(new ArrayList<>());
        fprInfo.setFilterTemplate(filterTemplate);

        Vulnerability vulnerability = new Vulnerability();
        vulnerability.setInstanceID(ISSUE_ID);
        vulnerability.setStackTrace(List.of(List.of(
                StackTraceElement.builder().filename(traceFile).line(30).build())));

        IssueAuditor auditor = IssueAuditor.builder()
                .vulnerabilities(List.of(vulnerability))
                .auditProcessor(auditProcessor)
                .auditIssueMap(auditIssueMap)
                .fprInfo(fprInfo)
                .filterSelection(new FilterSelection(null, null))
                .sourceLanguageResolver(new SourceLanguageResolver(new FVDLMetadata()))
                .options(AuditFprOptions.builder()
                        .fprHandle(fprHandle)
                        .logger(NO_OP_LOGGER)
                        .sscAppName("TestApp")
                        .sscAppVersion("1.0")
                        .forceReaudit(forceReaudit)
                        .allIssues(allIssues)
                        .build())
                .build();
        Method prepareMethod = IssueAuditor.class.getDeclaredMethod("prepareAndFilterPrompts");
        prepareMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentLinkedDeque<UserPrompt> prompts = (ConcurrentLinkedDeque<UserPrompt>) prepareMethod.invoke(auditor);
        return new ArrayList<>(prompts);
    }

    private void save() throws Exception {
        auditProcessor.updateAndSaveAuditAndRemediationsXml(Map.of(), tagMapping(), Map.of(), new FPRInfo(fprHandle));
    }

    private static Map<String, String> aviatorTags() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put(Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);
        return tags;
    }

    private static List<History> aviatorHistory() {
        return List.of(new History(Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR, Constants.USER_NAME));
    }

    private static String emptyAuditXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns2:Audit xmlns:ns2="%s" version="4.4"><ns2:IssueList/></ns2:Audit>
                """.formatted(AUDIT_NS);
    }

    private static String issueXml(String id, boolean suppressed, Map<String, String> tags, List<History> history) {
        StringBuilder issue = new StringBuilder();
        issue.append("<ns2:Issue instanceId=\"").append(id).append("\" revision=\"0\" suppressed=\"")
                .append(suppressed).append("\">");
        tags.forEach((tagId, value) -> issue.append("<ns2:Tag id=\"").append(tagId).append("\"><ns2:Value>")
                .append(value).append("</ns2:Value></ns2:Tag>"));
        if (!history.isEmpty()) {
            issue.append("<ns2:ClientAuditTrail>");
            history.forEach(entry -> issue.append("<ns2:TagHistory><ns2:Tag id=\"").append(entry.tagId())
                    .append("\"><ns2:Value>").append(entry.value()).append("</ns2:Value></ns2:Tag>")
                    .append("<ns2:EditTime>2026-01-01T00:00:00.000+00:00</ns2:EditTime><ns2:Username>")
                    .append(entry.user()).append("</ns2:Username></ns2:TagHistory>"));
            issue.append("</ns2:ClientAuditTrail>");
        }
        issue.append("</ns2:Issue>");
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns2:Audit xmlns:ns2=\"" + AUDIT_NS
                + "\" version=\"4.4\"><ns2:IssueList>" + issue + "</ns2:IssueList></ns2:Audit>";
    }

    private static String numbered(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append("line ").append(i).append('\n');
        }
        return sb.toString();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static AuditResponse successResponse(String instanceId) {
        return AuditResponse.builder()
                .issueId(instanceId)
                .status("SUCCESS")
                .tier("GOLD")
                .auditResult(AuditResult.builder().tagValue(Constants.NOT_AN_ISSUE).comment("Reviewed by Aviator").build())
                .build();
    }

    private static TagMappingConfig tagMapping() {
        TagMappingConfig config = new TagMappingConfig();
        TagMappingConfig.Mapping mapping = new TagMappingConfig.Mapping();
        mapping.setTier_1(tier(true));
        mapping.setTier_2(tier(false));
        config.setMapping(mapping);
        config.validate();
        return config;
    }

    private static TagMappingConfig.Tier tier(boolean suppressFalsePositives) {
        TagMappingConfig.Tier tier = new TagMappingConfig.Tier();
        tier.setFp(result("Not an Issue", suppressFalsePositives));
        tier.setTp(result("Exploitable", false));
        tier.setUnsure(result(null, false));
        return tier;
    }

    private static TagMappingConfig.Result result(String value, boolean suppress) {
        TagMappingConfig.Result result = new TagMappingConfig.Result();
        result.setValue(value);
        result.setSuppress(suppress);
        return result;
    }

    // ---------------------------------------------------------------- reading the saved audit.xml

    private Document savedAudit() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try (var in = Files.newInputStream(fprHandle.getPath("/audit.xml"))) {
            return factory.newDocumentBuilder().parse(in);
        }
    }

    private List<String> savedIssueIds() throws Exception {
        List<String> ids = new ArrayList<>();
        NodeList nodes = savedAudit().getElementsByTagNameNS(AUDIT_NS, "Issue");
        for (int i = 0; i < nodes.getLength(); i++) {
            ids.add(((Element) nodes.item(i)).getAttribute("instanceId"));
        }
        return ids;
    }

    private Element savedIssue(String id) throws Exception {
        NodeList nodes = savedAudit().getElementsByTagNameNS(AUDIT_NS, "Issue");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element issue = (Element) nodes.item(i);
            if (id.equals(issue.getAttribute("instanceId"))) {
                return issue;
            }
        }
        throw new AssertionError("Issue not found in saved audit.xml: " + id);
    }

    private static String liveTagValue(Element issue, String tagId) {
        for (Node child = issue.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element tag && "Tag".equals(tag.getLocalName()) && tagId.equalsIgnoreCase(tag.getAttribute("id"))) {
                return tag.getElementsByTagNameNS(AUDIT_NS, "Value").item(0).getTextContent();
            }
        }
        return null;
    }

    private static List<String> historyUsers(Element issue, String tagId) {
        List<String> users = new ArrayList<>();
        NodeList histories = issue.getElementsByTagNameNS(AUDIT_NS, "TagHistory");
        for (int i = 0; i < histories.getLength(); i++) {
            Element history = (Element) histories.item(i);
            Element tag = (Element) history.getElementsByTagNameNS(AUDIT_NS, "Tag").item(0);
            if (tagId.equalsIgnoreCase(tag.getAttribute("id"))) {
                users.add(history.getElementsByTagNameNS(AUDIT_NS, "Username").item(0).getTextContent());
            }
        }
        return users;
    }

    private static List<String> comments(Element issue) {
        List<String> comments = new ArrayList<>();
        NodeList contents = issue.getElementsByTagNameNS(AUDIT_NS, "Content");
        for (int i = 0; i < contents.getLength(); i++) {
            comments.add(contents.item(i).getTextContent());
        }
        return comments;
    }
}
