package com.fortify.cli.aviator.audit;

import static com.fortify.cli.aviator.util.Constants.DEFAULT_PING_INTERVAL_SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fortify.cli.aviator.audit.model.FilterSelection;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FolderDefinition;
import com.fortify.cli.aviator.fpr.filter.SearchTree;
import com.fortify.cli.aviator.fpr.filter.TagDefinition;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.audit.model.AuditOutcome;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper;
import com.fortify.cli.aviator.util.Constants;


public class IssueAuditor {

    private static final Logger LOG = LoggerFactory.getLogger(IssueAuditor.class);

    public final int MAX_PER_CATEGORY;
    public final int MAX_TOTAL;
    public final String MAX_PER_CATEGORY_EXCEEDED;
    public final String MAX_TOTAL_EXCEEDED;
    private final TagDefinition resultsTag;

    public final String SSCApplicationName;
    public final String SSCApplicationVersion;

    private List<Vulnerability> vulnerabilities;
    private List<UserPrompt> userPrompts;
    private final AuditProcessor auditProcessor;
    private final Map<String, AuditIssue> auditIssueMap;
    private final FPRInfo fprInfo;
    private final FilterSelection filterSelection;


    private final TagDefinition analysisTag;
    private TagDefinition humanAuditTag;
    private TagDefinition aviatorStatusTag;

    private final IAviatorLogger logger;

    public IssueAuditor(List<Vulnerability> vulnerabilities, AuditProcessor auditProcessor, Map<String, AuditIssue> auditIssueMap, FPRInfo fprInfo, String SSCApplicationName, String SSCApplicationVersion, FilterSelection filterSelection , IAviatorLogger logger) {
        this.logger = logger;
        this.MAX_PER_CATEGORY = Constants.MAX_PER_CATEGORY;
        this.MAX_TOTAL = Constants.MAX_TOTAL;
        this.MAX_PER_CATEGORY_EXCEEDED = Constants.MAX_PER_CATEGORY_EXCEEDED;
        this.MAX_TOTAL_EXCEEDED = Constants.MAX_TOTAL_EXCEEDED;
        this.vulnerabilities = vulnerabilities;
        this.userPrompts = new ArrayList<>();
        this.auditProcessor = auditProcessor;
        this.auditIssueMap = auditIssueMap;
        this.fprInfo = fprInfo;
        this.filterSelection = filterSelection;
        this.SSCApplicationName = SSCApplicationName;
        this.SSCApplicationVersion = SSCApplicationVersion;
        this.analysisTag = fprInfo.getFilterTemplate().getTagDefinitions().stream().filter(t -> "Analysis".equalsIgnoreCase(t.getName())).findFirst().orElse(null);
        this.resultsTag = resolveResultTag("", "", analysisTag);
    }

    private TagDefinition resolveResultTag(String tagName, String tagGuid, TagDefinition analysisTag) {
        Optional<TagDefinition> existingTag = fprInfo.getFilterTemplate().getTagDefinitions().stream().filter(t -> t.getName().equalsIgnoreCase(tagName)).findFirst();

        if (existingTag.isPresent()) {
            return existingTag.get();
        }

        if (analysisTag != null) {
            return analysisTag;
        }

        List<String> values;
        values = Arrays.asList(Constants.NOT_AN_ISSUE, Constants.EXPLOITABLE);

        return new TagDefinition(tagName, StringUtil.isEmpty(tagGuid) ? UUID.randomUUID().toString() : tagGuid, values, false);
    }

    private TagDefinition resolveAviatorPredictionTag() {
        String name = "Aviator prediction";
        String id = "C2D6EC66-CCB3-4FB9-9EE0-0BB02F51008F";

        List<String> values = Arrays.asList(Constants.AVIATOR_NOT_AN_ISSUE, Constants.AVIATOR_REMEDIATION_REQUIRED, Constants.AVIATOR_UNSURE, Constants.AVIATOR_EXCLUDED, Constants.AVIATOR_LIKELY_TP, Constants.AVIATOR_LIKELY_FP);
        return new TagDefinition(name, id, values, false);
    }

    private TagDefinition resolveAviatorStatusTag() {
        String name = "Aviator status";
        String id = "FB7B0462-2C2E-46D9-811A-DCC1F3C83051";

        List<String> values = List.of(Constants.PROCESSED_BY_AVIATOR);
        return new TagDefinition(name, id, values, false);
    }

    private TagDefinition resolveHumanAuditStatus() {
        String name = "FoD";
        String id = "604f0fbe-b5fe-47cd-a9cb-587ad8ebe93a";

        List<String> values = Arrays.asList(Constants.PENDING_REVIEW, Constants.FALSE_POSITIVE, Constants.EXPLOITABLE, Constants.SUSPICIOUS, Constants.SANITIZED);
        return new TagDefinition(name, id, values, false);
    }

    public AuditOutcome performAudit(Map<String, AuditResponse> auditResponses, String token,
                                     String projectName, String projectBuildId, String url) {
        projectName = StringUtil.isEmpty(projectName) ? projectBuildId : projectName;
        logger.progress("Starting audit for project: %s", projectName);

        ConcurrentLinkedDeque<UserPrompt> promptsToAudit = prepareAndFilterPrompts();
        int totalIssuesToAudit = promptsToAudit.size();
        logger.progress("Final count of issues to be audited: %d", totalIssuesToAudit);

        if (promptsToAudit.isEmpty()) {
            logger.progress("Audit skipped - no issues to process after filtering.");
        } else {
            try (AviatorGrpcClient client = AviatorGrpcClientHelper.createClient(url, logger, DEFAULT_PING_INTERVAL_SECONDS)) {
                CompletableFuture<Map<String, AuditResponse>> future =
                        client.processBatchRequests(promptsToAudit, projectName, fprInfo.getBuildId(), SSCApplicationName, SSCApplicationVersion, token);
                Map<String, AuditResponse> responses = future.get(500, TimeUnit.MINUTES);
                responses.forEach((requestId, response) -> auditResponses.put(response.getIssueId(), response));
                logger.progress("Audit completed");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AviatorSimpleException) {
                    throw (AviatorSimpleException) cause;
                } else if (cause instanceof AviatorTechnicalException) {
                    throw (AviatorTechnicalException) cause;
                } else {
                    throw new AviatorTechnicalException("Unexpected error during audit execution", cause);
                }
            } catch (TimeoutException e) {
                logger.error("Audit failed due to timeout after 500 minutes");
                throw new AviatorTechnicalException("Audit timed out after 500 minutes", e);
            } catch (InterruptedException e) {
                logger.error("Audit failed due to interruption");
                Thread.currentThread().interrupt();
                throw new AviatorTechnicalException("Audit interrupted", e);
            }
        }

        if (resultsTag != null) {
            fprInfo.setResultsTag(resultsTag.getId());
        }
        return new AuditOutcome(auditResponses, totalIssuesToAudit);
    }

    private ConcurrentLinkedDeque<UserPrompt> prepareAndFilterPrompts() {
        aviatorStatusTag = resolveAviatorStatusTag();
        humanAuditTag = resolveHumanAuditStatus();

        FilterSet activeFilterSet = filterSelection.getActiveFilterSet();
        List<Vulnerability> filteredVulnerabilities;

        if (activeFilterSet != null) {
            filteredVulnerabilities = filterVulnerabilities(this.vulnerabilities, activeFilterSet);
        } else {
            LOG.info("No active filter set. All applicable issues will be considered.");
            filteredVulnerabilities = new ArrayList<>(this.vulnerabilities);
        }

        // Convert the filtered vulnerabilities to UserPrompts
        List<UserPrompt> prompts = filteredVulnerabilities.stream()
                .map(IssueObjBuilder::buildIssueObj)
                .collect(Collectors.toList());

        // Apply secondary checks (like 'isAudited')

        return prompts.stream()
                .filter(this::shouldInclude).collect(Collectors.toCollection(ConcurrentLinkedDeque::new));

    }


    private boolean shouldInclude(UserPrompt userPrompt) {

        if (isAudited(userPrompt)) {
            LOG.debug("Skipping already audited issue ID: {}", userPrompt.getIssueData().getInstanceID());
            return false;
        }

        if (humanAuditTag != null) {
            String issueId = userPrompt.getIssueData().getInstanceID();
            String status = Optional.ofNullable(auditIssueMap.get(issueId)).map(AuditIssue::getTags).map(tags -> tags.get("604f0fbe-b5fe-47cd-a9cb-587ad8ebe93a")).orElse(null);
            if (!StringUtil.isEmpty(status) && !Constants.PENDING_REVIEW.equalsIgnoreCase(status)) {
                LOG.debug("Skipping because already manually audited: {}", issueId);
                return false;
            }
        }

        if (aviatorStatusTag != null) {
            String issueId = userPrompt.getIssueData().getInstanceID();
            String status = Optional.ofNullable(auditIssueMap.get(issueId)).map(AuditIssue::getTags).map(tags -> tags.get("FB7B0462-2C2E-46D9-811A-DCC1F3C83051")).orElse(null);
            if (!StringUtil.isEmpty(status) && Constants.PROCESSED_BY_AVIATOR.equalsIgnoreCase(status)) {
                LOG.debug("Skipping already PROCESSED_BY_AVIATOR: {}", issueId);
                return false;
            }
        }

        return true;
    }

    private boolean isAudited(UserPrompt userPrompt) {
        String issueId = userPrompt.getIssueData().getInstanceID();
        AuditIssue auditIssue = auditIssueMap.get(issueId);

        if (auditIssue == null) return false;

        Map<String, String> tags = auditIssue.getTags();
        if (tags == null) return false;

        String auditorStatusTag = Constants.AUDITOR_STATUS_TAG_ID;

        String auditorStatusValue = tags.get(auditorStatusTag);
        if (auditorStatusValue != null && !auditorStatusValue.equalsIgnoreCase("Pending Review")) {
            return true;
        }

        String aviatorExpectedOutcome = Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID;
        String analysisTagS = Constants.ANALYSIS_TAG_ID;

        if (auditIssueMap.containsKey(issueId)) {
            if (auditIssue.isSuppressed()){
                return true;
            }
            if (tags.containsKey(aviatorExpectedOutcome)) {
                return true;
            }

            if (analysisTag != null && tags.containsKey(analysisTag.getId())) {
                String tagValue = tags.get(analysisTag.getId());
                if (tagValue != null && !tagValue.equalsIgnoreCase("Not Set") && !tagValue.equalsIgnoreCase(Constants.PENDING_REVIEW) && !tagValue.trim().isEmpty()) {
                    return true;
                }
            }

            if (tags.containsKey(analysisTagS) && !tags.get(analysisTagS).equalsIgnoreCase("Not Set") && !StringUtil.isEmpty(tags.get(analysisTagS))) {
                return true;
            }
        }
        return false;
    }
    private List<Vulnerability> filterVulnerabilities(List<Vulnerability> allVulnerabilities, FilterSet fs) {
        List<String> targetFolderNames = filterSelection.getTargetFolderNames();

        List<Filter> folderFilters = fs.getFilters().stream()
                .filter(f -> "setFolder".equalsIgnoreCase(f.getAction())).collect(Collectors.toList());
        List<Filter> hideFilters = fs.getFilters().stream()
                .filter(f -> "hide".equalsIgnoreCase(f.getAction())).collect(Collectors.toList());

        Map<Filter, SearchTree> parsedQueries = new HashMap<>();
        // Use a stream to populate the map
        Stream.concat(folderFilters.stream(), hideFilters.stream()).forEach(f -> {
            try {
                parsedQueries.put(f, com.fortify.cli.aviator.fpr.filter.engine.FilterParser.parse(f.getQuery()));
            } catch (Exception e) {
                LOG.error("Failed to parse filter query: '{}'. This filter will be skipped.", f.getQuery(), e);
                parsedQueries.put(f, new com.fortify.cli.aviator.fpr.filter.SearchTree(null));
            }
        });

        Map<String, List<Vulnerability>> folderContents = new HashMap<>();
        for (Vulnerability vuln : allVulnerabilities) {
            for (Filter folderFilter : folderFilters) {
                if (com.fortify.cli.aviator.fpr.filter.engine.VulnerabilityEvaluator.evaluate(parsedQueries.get(folderFilter), vuln)) {
                    folderContents.computeIfAbsent(folderFilter.getActionParam(), k -> new ArrayList<>()).add(vuln);
                    break;
                }
            }
        }

        folderContents.values().forEach(vulnList ->
                vulnList.removeIf(vuln -> hideFilters.stream().anyMatch(
                        hideFilter -> com.fortify.cli.aviator.fpr.filter.engine.VulnerabilityEvaluator.evaluate(parsedQueries.get(hideFilter), vuln)
                ))
        );

        if (!filterSelection.isFilteringByFolder()) {
            List<Vulnerability> result = folderContents.values().stream().flatMap(List::stream).collect(Collectors.toList());
            logger.info("FilterSet '{}' applied. {} of {} total vulnerabilities remain.", fs.getTitle(), result.size(), allVulnerabilities.size());
            return result;
        } else {
            // This logic correctly finds folder IDs based on user-provided names.
            Set<String> targetFolderIds = fs.getFolderDefinitions().stream()
                    .filter(fd -> targetFolderNames.stream().anyMatch(name -> name.equalsIgnoreCase(fd.getName())))
                    .map(FolderDefinition::getId)
                    .collect(Collectors.toSet());

            if (targetFolderIds.isEmpty()) {
                String available = fs.getFolderDefinitions().stream().map(FolderDefinition::getName).collect(Collectors.joining("', '", "'", "'"));
                throw new AviatorSimpleException("Folder(s) not found in FilterSet '"+fs.getTitle()+"'. Available folders: "+available);
            }

            List<Vulnerability> result = folderContents.entrySet().stream()
                    .filter(entry -> targetFolderIds.contains(entry.getKey()))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList());

            logger.info("Filtered by folder(s) '{}'. {} vulnerabilities remain.", targetFolderNames, result.size());
            return result;
        }
    }

    private void updateSkippedIssue(UserPrompt userPrompt, String reasonTemplate, int issuesInCategory, int totalIssues) {
        if (userPrompt == null || userPrompt.getIssueData() == null) {
            LOG.error("Cannot update skipped issue status for null userPrompt or issue data.");
            return;
        }
        String instanceId = userPrompt.getIssueData().getInstanceID();
        AuditIssue auditIssue = auditIssueMap.get(instanceId);

        String comment = reasonTemplate
                .replace("{issues_new_in_category}", String.valueOf(issuesInCategory))
                .replace("{MAX_PER_CATEGORY}", String.valueOf(MAX_PER_CATEGORY))
                .replace("{issues_new_total}", String.valueOf(totalIssues))
                .replace("{MAX_TOTAL}", String.valueOf(MAX_TOTAL));

        if (auditIssue != null) {
            LOG.debug("Updating existing audit entry for skipped issue: {}", instanceId);
            try {
                auditProcessor.addCommentToIssueXml(instanceId, comment, Constants.USER_NAME);
            } catch (Exception e) {
                LOG.error("Failed to add comment to XML for skipped issue {}: {}", instanceId, e.getMessage());
            }

            auditProcessor.updateIssueTag(auditIssue, Constants.AVIATOR_STATUS_TAG_ID, Constants.PROCESSED_BY_AVIATOR);

            Map<String, String> tags = auditIssue.getTags();
            String currentAnalysisStatus = tags.get(Constants.ANALYSIS_TAG_ID);
            if (currentAnalysisStatus == null || currentAnalysisStatus.isEmpty() || "Not Set".equalsIgnoreCase(currentAnalysisStatus)) {
                auditProcessor.updateIssueTag(auditIssue, Constants.ANALYSIS_TAG_ID, Constants.PENDING_REVIEW);
                LOG.debug("Set Analysis tag to Pending Review for skipped issue: {}", instanceId);
            } else {
                LOG.debug("Skipped issue {} already has Analysis status '{}', not changing.", instanceId, currentAnalysisStatus);
            }

        } else {
            try {
                auditProcessor.addSkippedIssueElement(instanceId, comment);
            } catch (Exception e) {
                LOG.error("Failed to add skipped issue element to audit.xml for {}: {}", instanceId, e.getMessage());
            }
        }
    }
}