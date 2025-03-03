package com.fortify.cli.aviator.core;

import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.core.model.UserPrompt;
import com.fortify.cli.aviator.core.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.AuditIssue;
import com.fortify.cli.aviator.fpr.AuditProcessor;
import com.fortify.cli.aviator.fpr.FPRInfo;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.Filter;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.TagDefinition;
import com.fortify.cli.aviator.fpr.filter.TagValue;
import com.fortify.cli.aviator.grpc.AviatorGrpcClient;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class IssueAuditor {

    private static final Logger LOG = LoggerFactory.getLogger(IssueAuditor.class);

    public final int MAX_PER_CATEGORY;
    public final int MAX_TOTAL;
    public final String MAX_PER_CATEGORY_EXCEEDED;
    public final String MAX_TOTAL_EXCEEDED;
    private final TagDefinition resultsTag;

    private List<Vulnerability> vulnerabilities;
    private List<UserPrompt> userPrompts;
    private final AuditProcessor auditProcessor;
    private final Map<String, AuditIssue> auditIssueMap;
    private final FPRInfo fprInfo;
    private final String userName = Constants.USER_NAME;

    private final TagDefinition analysisTag;
    private TagDefinition humanAuditTag;
    private TagDefinition aviatorPredictionTag;
    private TagDefinition aviatorStatusTag;

    private final boolean isTestMode;
    private final AtomicInteger issuesSentToLLM;
    private final IAviatorLogger logger;

    public IssueAuditor(List<Vulnerability> vulnerabilities, AuditProcessor auditProcessor, Map<String, AuditIssue> auditIssueMap, FPRInfo fprInfo, boolean isTestMode, IAviatorLogger logger) {
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
        this.isTestMode = isTestMode;
        this.analysisTag = fprInfo.getFilterTemplate().getTagDefinitions().stream().filter(t -> "Analysis".equalsIgnoreCase(t.getName())).findFirst().orElse(null);
        this.resultsTag = resolveResultTag("", "", analysisTag);

        issuesSentToLLM = new AtomicInteger(0);
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
        if (analysisTag != null) {
            values = analysisTag.getValues().stream().map(TagValue::getValue).collect(Collectors.toList());
        } else {
            values = Arrays.asList(Constants.NOT_AN_ISSUE, Constants.EXPLOITABLE);
        }

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

        List<String> values = Arrays.asList(Constants.PROCESSED_BY_AVIATOR);
        return new TagDefinition(name, id, values, false);
    }

    private TagDefinition resolveHumanAuditStatus() {
        String name = "FoD";
        String id = "604f0fbe-b5fe-47cd-a9cb-587ad8ebe93a";

        List<String> values = Arrays.asList(Constants.PENDING_REVIEW, Constants.FALSE_POSITIVE, Constants.EXPLOITABLE, Constants.SUSPICIOUS, Constants.SANITIZED);
        return new TagDefinition(name, id, values, false);
    }

    public void performAudit(Map<String, AuditResponse> auditResponses, String token, String projectName,String projectBuildId, String url) {
        projectName = StringUtil.isEmpty(projectName) ? projectBuildId : projectName;
        logger.progress("Starting audit for project: %s", projectName);

        aviatorPredictionTag = resolveAviatorPredictionTag();
        aviatorStatusTag = resolveAviatorStatusTag();
        humanAuditTag = resolveHumanAuditStatus();
        LOG.debug("Initialized tags - prediction: {}, status: {}, human: {}", aviatorPredictionTag, aviatorStatusTag, humanAuditTag);

        if (fprInfo.getDefaultEnabledFilterSet() != null) {
            vulnerabilities = filterVulnerabilities(vulnerabilities, fprInfo.getDefaultEnabledFilterSet());
        }

        vulnerabilities.stream().map(IssueObjBuilder::buildIssueObj).forEach(userPrompts::add);
        LOG.info("Built {} user prompts from vulnerabilities", userPrompts.size());
        ConcurrentLinkedDeque<UserPrompt> filteredUserPrompts = getIssuesToAudit();
        logger.progress("Filtered issues count: %d", filteredUserPrompts.size());
        try (AviatorGrpcClient client = createClientFromUrl(url)) {
            CompletableFuture<Map<String, AuditResponse>> future = client.processBatchRequests(filteredUserPrompts, projectName, token);
            try {
                Map<String, AuditResponse> responses = future.get(500, TimeUnit.MINUTES);
                responses.forEach((requestId, response) -> {
                    auditResponses.put(response.getIssueId(), response);
                });
            } catch (ExecutionException e) {
                LOG.error("Error executing requests: {} ", e.getCause());
            } catch (TimeoutException e) {
                LOG.error("Error executing requests:timeout {}", String.valueOf(e.getCause()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("Error executing requests:interrupted {}", e.getCause());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(":::::::::::::: result id "+resultsTag.getId());
        logger.progress(":::::::::::::: result id "+resultsTag.getId());
        fprInfo.setResultsTag(resultsTag.getId());
        logger.progress("Audit completed");
    }

    private ConcurrentLinkedDeque<UserPrompt> getIssuesToAudit() {
        userPrompts = userPrompts.stream().filter(userPrompt -> shouldInclude(userPrompt)).collect(Collectors.toList());
        Map<String, List<UserPrompt>> issuesByCategory = userPrompts.stream().collect(Collectors.groupingBy(UserPrompt::getCategory));

        Map<String, Integer> newIssuesInCategoryCapped = new HashMap<>();
        int totalNewIssues = 0;
        int totalNewIssuesCapped = 0;

        for (Map.Entry<String, List<UserPrompt>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            List<UserPrompt> issues = entry.getValue();
            issues.sort(new IssueOrderingComparator());

            issues = issues.stream().filter(issue -> shouldInclude(issue)).collect(Collectors.toList());
            int newIssuesCount = issues.size();
            int cappedCount = Math.min(MAX_PER_CATEGORY, newIssuesCount);

            newIssuesInCategoryCapped.put(category, cappedCount);
            totalNewIssues += newIssuesCount;
            totalNewIssuesCapped += cappedCount;
        }

        if (totalNewIssuesCapped <= MAX_TOTAL) {
            return getIssuesToAuditLimitedByCategoryCount(issuesByCategory, totalNewIssues);
        }

        double auditAllThreshold = totalNewIssuesCapped / (2.0 * issuesByCategory.size());
        int auditAllTotal = 0;
        int auditSomeTotal = 0;

        for (int count : newIssuesInCategoryCapped.values()) {
            if (count < auditAllThreshold) {
                auditAllTotal += count;
            } else {
                auditSomeTotal += count;
            }
        }

        ConcurrentLinkedDeque<UserPrompt> issuesToAudit = new ConcurrentLinkedDeque<>();
        double auditFraction = ((double) MAX_TOTAL - (double) auditAllTotal) / (double) auditSomeTotal;

        for (Map.Entry<String, List<UserPrompt>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            List<UserPrompt> issues = entry.getValue();
            long totalLimitIndex = Math.round(auditFraction * newIssuesInCategoryCapped.get(category));
            int i = 0;

            for (UserPrompt issue : issues) {
                if (i >= MAX_PER_CATEGORY) {
                    updateSkippedIssue(issue, MAX_PER_CATEGORY_EXCEEDED, issues.size(), totalNewIssues);
                } else if (i > auditAllThreshold && i >= totalLimitIndex) {
                    updateSkippedIssue(issue, MAX_TOTAL_EXCEEDED, issues.size(), totalNewIssues);
                } else {
                    issuesToAudit.add(issue);
                }
                i++;
            }
        }

        return issuesToAudit;
    }

    private ConcurrentLinkedDeque<UserPrompt> getIssuesToAuditLimitedByCategoryCount(Map<String, List<UserPrompt>> allUnseenIssues, int totalCount) {
        ConcurrentLinkedDeque<UserPrompt> issuesToAudit = new ConcurrentLinkedDeque<>();

        for (Map.Entry<String, List<UserPrompt>> entry : allUnseenIssues.entrySet()) {
            List<UserPrompt> issues = entry.getValue();

            if (issues.size() <= MAX_PER_CATEGORY) {
                issuesToAudit.addAll(issues);
            } else {
                for (int i = 0; i < issues.size(); i++) {
                    if (i < MAX_PER_CATEGORY){
                        issuesToAudit.add(issues.get(i));
                    } else{
                        updateSkippedIssue(issues.get(i), MAX_PER_CATEGORY_EXCEEDED, issues.size(), totalCount);
                    }
                }
            }
        }
        return issuesToAudit;
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
        String auditorStatusTag = Constants.AUDITOR_STATUS_TAG_ID;
        Boolean isAuditorStatusPopulated = tags.containsKey(auditorStatusTag) && tags.get(auditorStatusTag).equalsIgnoreCase("Pending Review");
        String aviatorExpectedOutcome = Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID;
        String analysisTagS = Constants.ANALYSIS_TAG_ID;

        if (auditIssueMap.containsKey(issueId)) {
            if (isAuditorStatusPopulated || tags.containsKey(aviatorExpectedOutcome)) {
                return true;
            }
            if (tags.containsKey(analysisTag.getId()) && !tags.get(analysisTag.getId()).equalsIgnoreCase("Not Set") && !tags.get(analysisTag.getId()).equalsIgnoreCase(Constants.PENDING_REVIEW) && !StringUtil.isEmpty(tags.get(analysisTag.getId()))) {
                return true;
            }
            if (tags.containsKey(analysisTagS) && !tags.get(analysisTagS).equalsIgnoreCase("Not Set") && !StringUtil.isEmpty(tags.get(analysisTagS))) {
                return true;
            }
        }
        return false;
    }

    public List<Vulnerability> filterVulnerabilities(List<Vulnerability> vulnerabilities, FilterSet fs) {
        if (fs == null || vulnerabilities == null) {
            return vulnerabilities;
        }

        Set<Vulnerability> resultSet = new HashSet<>();

        for (Filter filter : fs.getFilters()) {
            String actionParam = filter.getActionParam();
            String query = filter.getQuery();
            String action = filter.getAction();

            if ("true".equalsIgnoreCase(actionParam)) {
                if ("setFolder".equalsIgnoreCase(action)) {
                    processSpecialQuery(vulnerabilities, query, resultSet, true);
                } else if ("hide".equalsIgnoreCase(action)) {
                    processSpecialQuery(vulnerabilities, query, resultSet, false);
                }
            } else if (StringUtil.isValidUUID(actionParam)) {
                if ("setFolder".equalsIgnoreCase(action)) {
                    processAdvancedQuery(vulnerabilities, query, resultSet, true);
                } else if ("hide".equalsIgnoreCase(action)) {
                    processAdvancedQuery(vulnerabilities, query, resultSet, false);
                }
            }
        }

        return new ArrayList<>(resultSet);
    }

    private void processAdvancedQuery(List<Vulnerability> vulnerabilities, String query, Set<Vulnerability> resultSet, boolean shouldAdd) {
        Pattern fortifyPriorityPattern = Pattern.compile("confidence:\\[(\\d+),(\\d+)]\\s*severity:\\((\\d+),(\\d+)]");
        Matcher fortifyPriorityMatcher = fortifyPriorityPattern.matcher(query);

        Pattern newPattern = Pattern.compile("confidence:\\[(\\d+(\\.\\d+)?)-(\\d+(\\.\\d+)?)]\\s*AND\\s*\\[fortify priority order]:(\\w+)");
        Matcher newMatcher = newPattern.matcher(query);

        Pattern fortifyPriorityOrderPattern = Pattern.compile("\\[fortify priority order]:(\\w+)");
        Matcher fortifyPriorityOrderMatcher = fortifyPriorityOrderPattern.matcher(query);

        if (newMatcher.matches()) {
            handleNewPatternMatching(vulnerabilities, resultSet, newMatcher, shouldAdd);
        } else if (fortifyPriorityMatcher.matches()) {
            handleFortifyPriorityMatching(vulnerabilities, resultSet, fortifyPriorityMatcher, shouldAdd);
        } else if (fortifyPriorityOrderMatcher.matches()) {
            handleFortifyPriorityOrderMatching(vulnerabilities, resultSet, fortifyPriorityOrderMatcher, shouldAdd);
        } else {
            handleDefaultCase(vulnerabilities, resultSet, shouldAdd);
        }
    }

    private void handleFortifyPriorityOrderMatching(List<Vulnerability> vulnerabilities, Set<Vulnerability> resultSet, Matcher matcher, boolean shouldAdd) {
        String priority = matcher.group(1).toUpperCase();

        vulnerabilities.stream().filter(vuln -> vuln.getPriority() != null && vuln.getPriority().equalsIgnoreCase(priority)).forEach(vuln -> {
            if (shouldAdd) {
                resultSet.add(vuln);
            } else {
                resultSet.remove(vuln);
            }
        });
    }

    private void handleNewPatternMatching(List<Vulnerability> vulnerabilities, Set<Vulnerability> resultSet, Matcher newMatcher, boolean shouldAdd) {
        double confidenceMin = Double.parseDouble(newMatcher.group(1));
        double confidenceMax = Double.parseDouble(newMatcher.group(3));
        String priority = newMatcher.group(5).toUpperCase();

        vulnerabilities.stream().filter(vuln -> {
            if (vuln.getConfidence() >= confidenceMin && vuln.getConfidence() <= confidenceMax) {
                return vuln.getPriority() != null && vuln.getPriority().toUpperCase().equals(priority);
            }
            return false;
        }).forEach(vuln -> {
            if (shouldAdd) {
                resultSet.add(vuln);
            } else {
                resultSet.remove(vuln);
            }
        });
    }

    private void handleFortifyPriorityMatching(List<Vulnerability> vulnerabilities, Set<Vulnerability> resultSet, Matcher fortifyPriorityMatcher, boolean shouldAdd) {
        int[] ranges = parseRanges(fortifyPriorityMatcher);

        if (ranges.length != 4) {
            return;
        }

        int confidenceMin = ranges[0];
        int confidenceMax = ranges[1];
        int severityMin = ranges[2];
        int severityMax = ranges[3];

        vulnerabilities.stream().filter(vuln -> vuln.getConfidence() >= confidenceMin && vuln.getConfidence() <= confidenceMax && vuln.getInstanceSeverity() > severityMin && vuln.getInstanceSeverity() <= severityMax).forEach(vuln -> {
            if (shouldAdd) {
                resultSet.add(vuln);
            } else {
                resultSet.remove(vuln);
            }
        });
    }

    private void handleDefaultCase(List<Vulnerability> vulnerabilities, Set<Vulnerability> resultSet, boolean shouldAdd) {
        vulnerabilities.forEach(vuln -> {
            if (shouldAdd) {
                resultSet.add(vuln);
            } else {
                resultSet.remove(vuln);
            }
        });
    }

    public static int[] parseRanges(Matcher matcher) {
        matcher.reset();
        if (matcher.find()) {
            return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))};
        }
        return new int[0];
    }

    private void processSpecialQuery(List<Vulnerability> vulnerabilities, String query, Set<Vulnerability> resultSet, boolean shouldAdd) {
        List<String> conditions = splitQueryIntoConditions(query);

        if (conditions.isEmpty()) {
            if (shouldAdd) {
                resultSet.addAll(vulnerabilities);
            }
            return;
        }

        for (Vulnerability vuln : vulnerabilities) {
            boolean matchesAllConditions = true;

            for (String condition : conditions) {
                if (!condition.contains(":")) continue;

                String[] parts = condition.split(":", 2);
                String field = parts[0].trim();
                String value = parts[1].trim();

                value = value.replace("\\:", ":");

                boolean isNegation = value.startsWith("!");
                String actualValue = isNegation ? value.substring(1) : value;

                boolean matches;
                if (field.equalsIgnoreCase("confidence") || field.equalsIgnoreCase("severity")) {
                    matches = checkRangeMatch(vuln, field, actualValue);
                } else {
                    matches = checkFieldMatch(vuln, field, actualValue);
                }

                if (isNegation) {
                    matches = !matches;
                }

                if (!matches) {
                    matchesAllConditions = false;
                    break;
                }
            }

            if (matchesAllConditions) {
                if (shouldAdd) {
                    resultSet.add(vuln);
                } else {
                    resultSet.remove(vuln);
                }
            }
        }
    }

    private boolean checkRangeMatch(Vulnerability vuln, String field, String range) {
        Pattern rangePattern = Pattern.compile("([\\[(])(\\d+(\\.\\d+)?),(\\d+(\\.\\d+)?)([])])");
        Matcher rangeMatcher = rangePattern.matcher(range);

        if (!rangeMatcher.matches()) {
            return false;
        }

        double value;
        if (field.equalsIgnoreCase("confidence")) {
            value = vuln.getConfidence();
        } else if (field.equalsIgnoreCase("severity")) {
            value = vuln.getInstanceSeverity();
        } else {
            return false;
        }

        double min = Double.parseDouble(rangeMatcher.group(2));
        double max = Double.parseDouble(rangeMatcher.group(4));
        boolean includeMin = rangeMatcher.group(1).equals("[");
        boolean includeMax = rangeMatcher.group(5).equals("]");

        if (includeMin && includeMax) {
            return value >= min && value <= max;
        } else if (includeMin) {
            return value >= min && value < max;
        } else if (includeMax) {
            return value > min && value <= max;
        } else {
            return value > min && value < max;
        }
    }

    private List<String> splitQueryIntoConditions(String query) {
        List<String> conditions = new ArrayList<>();
        Pattern conditionPattern = Pattern.compile("(\\w+):(.*?)(?=\\s\\w+:|$)");
        Matcher matcher = conditionPattern.matcher(query);

        while (matcher.find()) {
            conditions.add(matcher.group().trim());
        }

        return conditions;
    }


    private boolean checkFieldMatch(Vulnerability vuln, String field, String value) {
        return switch (field.toLowerCase()) {
            case "audience" ->
                    vuln.getAudience() != null && vuln.getAudience().toLowerCase().contains(value.toLowerCase());
            case "analyzer" -> vuln.getAnalyzer() != null && vuln.getAnalyzer().equalsIgnoreCase(value.toLowerCase());
            case "category" -> vuln.getCategory() != null && vuln.getCategory().equalsIgnoreCase(value.toLowerCase());
            case "fortify priority order" -> vuln.getPriority() != null && vuln.getPriority().equalsIgnoreCase(value);
            default -> false;
        };
    }

    private void updateSkippedIssue(UserPrompt userPrompt, String reason, int... values) {
        AuditIssue auditIssue = auditIssueMap.get(userPrompt.getIssueData().getInstanceID());

        if (auditIssue != null) {
            String comment = formatComment(reason, values);

            AuditIssue.Comment skippedComment = AuditIssue.Comment.builder()
                    .content(comment)
                    .username(userName)
                    .timestamp(new Date().toString())
                    .build();

            auditIssue.getThreadedComments().add(skippedComment);
            auditProcessor.updateIssueTag(auditIssue, "FB7B0462-2C2E-46D9-811A-DCC1F3C83051", Constants.PROCESSED_BY_AVIATOR);
        }
    }

    private String formatComment(String template, int... values) {
        if (values.length == 2) {
            template = template.replace("{issues_new_in_category}", String.valueOf(values[0])).replace("{MAX_PER_CATEGORY}", String.valueOf(MAX_PER_CATEGORY)).replace("{issues_new_total}", String.valueOf(values[1])).replace("{MAX_TOTAL}", String.valueOf(MAX_TOTAL));
        }
        return template;
    }


    private class IssueOrderingComparator implements Comparator<UserPrompt> {
        @Override
        public int compare(UserPrompt first, UserPrompt second) {
            String firstFilename = Optional.ofNullable(first.getLastStackTraceElement()).map(StackTraceElement::getFilename).orElse("");
            String secondFilename = Optional.ofNullable(second.getLastStackTraceElement()).map(StackTraceElement::getFilename).orElse("");

            int filenameComparison = firstFilename.compareTo(secondFilename);
            if (filenameComparison != 0) {
                return filenameComparison;
            }

            Integer firstLine = Optional.ofNullable(first.getLastStackTraceElement()).map(StackTraceElement::getLine).orElse(0);
            Integer secondLine = Optional.ofNullable(second.getLastStackTraceElement()).map(StackTraceElement::getLine).orElse(0);

            return Integer.compare(firstLine, secondLine);
        }
    }

    public AviatorGrpcClient createClientFromUrl(String url) {
        try {
            String host;
            int port;

            if (!url.contains("://")) {
                String[] parts = url.split(":");
                host = parts[0];
                port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9090;
            } else {
                URI uri = new URI(url);
                host = uri.getHost();
                port = uri.getPort();
                if (port == -1) {
                    port = 9090;
                }
            }

            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid host in URL: " + url);
            }

            return new AviatorGrpcClient(host, port, 10, logger);
        } catch (URISyntaxException | NumberFormatException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }
}