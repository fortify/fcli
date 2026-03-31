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
package com.fortify.cli.ssc.issue.cli.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.issue.helper.SSCIssueCustomTagAuditValue;
import com.fortify.cli.ssc.issue.helper.SSCIssueCustomTagHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueIdentifier;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Update.CMD_NAME)
//@Slf4j
public class SSCIssueUpdateCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    
    @Getter @Mixin private OutputHelperMixins.Update outputHelper;
    @Mixin private SSCAppVersionResolverMixin.RequiredOption appVersionResolver;
    @Option(names = {"--issue-ids"}, required = true, split = ",")
    private List<String> issueIds;
    @Option(names = {"--custom-tags", "-t"}, split = ",", paramLabel = "TAG=VALUE")
    private Map<String,String> customTags;
    @Option(names = {"--suppress"}, arity = "1", paramLabel = "true|false")
    private Boolean suppress;
    @Option(names = {"--comment"})
    private String comment;
    @Option(names = {"--assign-user"})
    private String assignUser;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validateInput();
        String appVersionId = appVersionResolver.getAppVersionId(unirest);
        List<SSCIssueIdentifier> issues = fetchIssueRevisionsFromSSC(unirest, appVersionId, issueIds);

        if (StringUtils.isNotBlank(assignUser)) {
            executeAssignUserRequest(unirest, appVersionId, issues, assignUser);
            if (isUpdateRequired()) {
                issues = fetchIssueRevisionsFromSSC(unirest, appVersionId, issueIds);
            }
        }

        if (isUpdateRequired()) {
            executeAuditRequest(unirest, appVersionId, issues);
        }

        return buildResults(unirest);
    }

    private void validateInput() {
        if (issueIds == null || issueIds.isEmpty()) {
            throw new FcliSimpleException("--issue-ids must be specified");
        }
        if (!isUpdateRequired() && StringUtils.isBlank(assignUser)) {
            throw new FcliSimpleException("At least one of --custom-tags, --suppress, --comment, or --assign-user must be specified");
        }
    }

    private boolean isUpdateRequired() {
        return hasCustomTags() || suppress != null || StringUtils.isNotBlank(comment);
    }
    
    private boolean hasCustomTags() {
        return customTags != null && !customTags.isEmpty();
    }

    private JsonNode buildResults(UnirestInstance unirest) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();

        String updatesSummary = buildUpdateDetails();

        ArrayNode issueIdsArray = result.putArray("issueIds");
        for (String vulnId : issueIds) {
            issueIdsArray.add(vulnId);
        }

        result.put("updatesString", updatesSummary);

        if (hasCustomTags()) {
            ArrayNode customTagsArray = result.putArray("customTagUpdates");
            String appVersionId = appVersionResolver.getAppVersionId(unirest);
            var customTagHelper = new SSCIssueCustomTagHelper(unirest, appVersionId);
            customTagHelper.populateCustomTagUpdates(customTags, customTagsArray);
        }

        if (StringUtils.isNotBlank(comment)) {
            result.put("newComment", comment);
        }

        if (StringUtils.isNotBlank(assignUser)) {
            result.put("assignedUser", assignUser);
        }

        if (suppress != null) {
            result.put("suppressed", suppress);
        }

        return result;
    }

    private String buildUpdateDetails() {
        StringBuilder details = new StringBuilder();
        if (hasCustomTags()) {
            customTags.forEach((key, value) -> 
                appendDetail(details, "CustomTag: " + key + "=" + (StringUtils.isBlank(value) ? "<unset>" : value)));
        }
        if (suppress != null) {
            appendDetail(details, "Suppressed: " + suppress);
        }
        if (StringUtils.isNotBlank(assignUser)) {
            appendDetail(details, "User: " + assignUser);
        }
        if (StringUtils.isNotBlank(comment)) {
            appendDetail(details, "Comment: " + comment);
        }
        String result = details.toString();
        return result.isEmpty() ? "No updates" : result;
    }
    
    private void appendDetail(StringBuilder sb, String detail) {
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(detail);
    }
    
    private void executeAssignUserRequest(UnirestInstance unirest, String appVersionId, 
            List<SSCIssueIdentifier> issues, String user) {
        ObjectNode requestBody = JsonHelper.getObjectMapper().createObjectNode();
        ArrayNode issuesArray = requestBody.putArray("issues");
        for (SSCIssueIdentifier issue : issues) {
            ObjectNode issueNode = JsonHelper.getObjectMapper().createObjectNode();
            issueNode.put("id", issue.id());
            issueNode.put("revision", issue.revision());
            issuesArray.add(issueNode);
        }
        requestBody.put("user", user);
        
        String url = SSCUrls.PROJECT_VERSION_ISSUES_ACTION_ASSIGN_USER(appVersionId);
        
        try {
            JsonNode response = unirest.post(url)
                    .body(requestBody)
                    .asObject(JsonNode.class)
                    .getBody();
            validateApiResponse(response, "Assign user operation");
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to assign user: " + e.getMessage(), e);
        }
    }
    
    private void executeAuditRequest(UnirestInstance unirest, String appVersionId, List<SSCIssueIdentifier> issues) {
        Map<String, Object> request = new HashMap<>();
        request.put("issues", issues);
        if (comment != null) {
            request.put("comment", comment);
        }
        if (suppress != null) {
            request.put("suppressed", suppress);
        }
        if (hasCustomTags()) {
            var customTagHelper = new SSCIssueCustomTagHelper(unirest, appVersionId);
            List<SSCIssueCustomTagAuditValue> processedTags = customTagHelper.processCustomTags(customTags);
            request.put("customTagAudit", processedTags);
        }
        
        String url = SSCUrls.PROJECT_VERSION_ISSUES_ACTION_AUDIT(appVersionId);
        
        try {
            JsonNode response = unirest.post(url)
                    .body(request)
                    .asObject(JsonNode.class)
                    .getBody();
            validateApiResponse(response, "Audit operation");
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to perform audit operation: " + e.getMessage(), e);
        }
    }
    
    private void validateApiResponse(JsonNode response, String operationName) {
        if (response == null) {
            throw new FcliSimpleException(operationName + " returned null response");
        }
        if (response.has("responseCode")) {
            int responseCode = response.get("responseCode").asInt();
            if (responseCode >= 400) {
                String message = response.has("message") ? response.get("message").asText() : "Unknown error";
                throw new FcliSimpleException(operationName + " failed with response code " + responseCode + ": " + message);
            }
        }
    }
    
    @Override
    public String getActionCommandResult() {
        return "UPDATED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private List<SSCIssueIdentifier> fetchIssueRevisionsFromSSC(UnirestInstance unirest, String appVersionId, List<String> issueIds) {
        String idsParam = String.join(",", issueIds);
        
        try {
            JsonNode response = unirest.get("/api/v1/projectVersions/{appVersionId}/issues")
                    .routeParam("appVersionId", appVersionId)
                    .queryString("ids", idsParam)
                    .asObject(JsonNode.class)
                    .getBody();
            
            JsonNode dataArray = response.get("data");
            if (dataArray == null || !dataArray.isArray()) {
                throw new FcliSimpleException("Invalid response from SSC issues API - missing 'data' field");
            }
            
            Map<String, Integer> idToRevisionMap = new HashMap<>();
            for (JsonNode issueNode : dataArray) {
                idToRevisionMap.put(issueNode.get("id").asText(), issueNode.get("revision").asInt());
            }
            
            for (String issueId : issueIds) {
                if (!idToRevisionMap.containsKey(issueId)) {
                    throw new FcliSimpleException("Issue with ID '" + issueId + "' not found in application version");
                }
            }
            
            return issueIds.stream()
                    .map(id -> SSCIssueIdentifier.fromIdAndRevision(id, idToRevisionMap.get(id)))
                    .toList();
            
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to fetch issue revisions from SSC: " + e.getMessage(), e);
        }
    }
}