/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc._common.rest;

public class SSCUrls {
    private static final String ApiBase = "/api/v1";
    public static final String ACTIVITY_FEED_EVENTS = ApiBase + "/activityFeedEvents";
    public static final String ALERT_DEFINITIONS = ApiBase + "/alertDefinitions";
    public static String ALERT_DEFINITION(String definitionId){
        return String.format(ApiBase + "/alertDefinitions/%s", definitionId);
    }
    public static String ALERT_DEFINITION_PROJECT_VERSIONS(String projectId){
        return String.format(ApiBase + "/alertDefinitions/%s/projectVersions", projectId);
    }
    public static final String ALERTABLE_EVENT_TYPES = ApiBase + "/alertableEventTypes";
    public static final String ALERTS = ApiBase + "/alerts";
    public static final String ALERTS_ACTION_SET_STATUS = ApiBase + "/alerts/action/setStatus";
    public static final String APPLICATION_STATE = ApiBase + "/applicationState";
    public static final String ARTIFACTS_ACTION_APPROVE = ApiBase + "/artifacts/action/approve";
    public static final String ARTIFACTS_ACTION_PURGE = ApiBase + "/artifacts/action/purge";
    public static String ARTIFACT(String artifactId) {
        return String.format(ApiBase + "/artifacts/%s", artifactId);
    }
    public static String ARTIFACT_SCAN_ERRORS(String artifactId) {
        return String.format(ApiBase + "/artifacts/%s/scanerrors", artifactId);
    }
    public static String ARTIFACT_SCANS(String artifactId) {
        return String.format(ApiBase + "/artifacts/%s/scans", artifactId);
    }
    public static final String ATTRIBUTE_DEFINITIONS = ApiBase + "/attributeDefinitions";
    public static String ATTRIBUTE_DEFINITION(String attrDefId) {
        return String.format(ApiBase + "/attributeDefinitions/%s", attrDefId);
    }
    public static final String AUTH_CLIENT_SESSION_ID = ApiBase + "/auth/clientSessionId";
    public static final String AUTH_ENTITIES = ApiBase + "/authEntities";
    public static String AUTH_ENTITY(String authEntityId) {
        return String.format(ApiBase + "/authEntities/%s", authEntityId);
    }
    public static String AUTH_ENTITY_GROUPS(String authEntityId) {
        return String.format(ApiBase + "/authEntities/%s/groups", authEntityId);
    }
    public static String AUTH_ENTITY_PROJECT_VERSIONS(String authEntityId) {
        return String.format(ApiBase + "/authEntities/%s/projectVersions", authEntityId);
    }
    public static String AUTH_ENTITY_PROJECT_VERSIONS_ACTION_ASSIGN(String authEntityId) {
        return String.format(ApiBase + "/authEntities/%s/projectVersions/action/assign", authEntityId);
    }
    public static String AUTH_ENTITY_ROLES(String authEntityId) {
        return String.format(ApiBase + "/authEntities/%s/roles", authEntityId);
    }
    public static final String BUG_FIELD_TEMPLATE_GROUPS = ApiBase + "/bugfieldTemplateGroups";
    public static String BUG_FIELD_TEMPLATE_GROUP(String bugFieldTemplateGroupId) {
        return String.format(ApiBase + "/bugfieldTemplateGroups/%s", bugFieldTemplateGroupId);
    }
    public static final String BUG_TRACKERS = ApiBase + "/bugtrackers";
    public static final String BULK = ApiBase + "/bulk";
    public static final String CLOUD_JOBS = ApiBase + "/cloudjobs";
    public static final String CLOUD_JOBS_ACTION_CANCEL = ApiBase + "/cloudjobs/action/cancel";
    public static String CLOUD_JOB(String cloudJobId) {
        return String.format(ApiBase + "/cloudjobs/%s", cloudJobId);
    }
    public static final String CLOUD_MAPPINGS_MAP_BY_VERSION_ID = ApiBase + "/cloudmappings/mapByVersionId";
    public static final String CLOUD_MAPPINGS_MAP_BY_VERSION_IDS = ApiBase + "/cloudmappings/mapByVersionIds";
    public static final String CLOUD_MAPPINGS_MAP_BY_VERSION_NAME = ApiBase + "/cloudmappings/mapByVersionName";
    public static final String CLOUD_POOLS = ApiBase + "/cloudpools";
    public static final String CLOUD_POOLS_DISABLED_WORKERS = ApiBase + "/cloudpools/disabledWorkers";
    public static String CLOUD_POOL_JOBS(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/jobs", cloudPoolUuid);
    }
    public static String CLOUD_POOL_VERSIONS(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/versions", cloudPoolUuid);
    }
    public static String CLOUD_POOL_VERSIONS_ACTION_ASSIGN(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/versions/action/assign", cloudPoolUuid);
    }
    public static String CLOUD_POOL_VERSIONS_ACTION_REPLACE(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/versions/action/replace", cloudPoolUuid);
    }
    public static String CLOUD_POOL_WORKERS(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/workers", cloudPoolUuid);
    }
    public static String CLOUD_POOL_WORKERS_ACTION_ASSIGN(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/workers/action/assign", cloudPoolUuid);
    }
    public static String CLOUD_POOL_WORKERS_ACTION_DISABLE(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/workers/action/disable", cloudPoolUuid);
    }
    public static String CLOUD_POOL_WORKERS_ACTION_REPLACE(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s/workers/action/replace", cloudPoolUuid);
    }
    public static String CLOUD_POOL(String cloudPoolUuid) {
        return String.format(ApiBase + "/cloudpools/%s", cloudPoolUuid);
    }
    public static final String CLOUD_SYSTEM_METRICS = ApiBase + "/cloudsystem/metrics";
    public static final String CLOUD_SYSTEM_POLLSTATUS = ApiBase + "/cloudsystem/pollstatus";
    public static final String CLOUD_SYSTEM_SETTINGS = ApiBase + "/cloudsystem/settings";
    public static final String CLOUD_SYSTEM_SETTINGS_ACTION_ACTIVATE = ApiBase + "/cloudsystem/settings/action/activate";
    public static final String CLOUD_SYSTEM_SETTINGS_ACTION_DEACTIVATE = ApiBase + "/cloudsystem/settings/action/deactivate";
    public static final String CLOUD_WORKERS = ApiBase + "/cloudworkers";
    public static final String CLOUD_WORKERS_ACTION_DEACTIVATE = ApiBase + "/cloudworkers/action/deactivate";
    public static String CLOUD_WORKER_CLOUDJOBS(String cloudWorkerId) {
        return String.format(ApiBase + "/cloudworkers/%s/cloudjobs", cloudWorkerId);
    }
    public static String CLOUD_WORKER(String cloudWorkerUuid) {
        return String.format(ApiBase + "/cloudworkers/%s", cloudWorkerUuid);
    }
    public static final String COMMENTS = ApiBase + "/comments";
    public static final String CONFIGURATIONS = ApiBase + "/configuration";
    public static final String CONFIGURATION_ACTION_REFRESH_AUDIT_ASSISTANT_POLICIES = ApiBase + "/configuration/action/refreshAuditAssistantPolicies";
    public static final String CONFIGURATION_CURRENT_AUTHENTICATION_INFO = ApiBase + "/configuration/currentAuthenticationInfo";
    public static final String CONFIGURATION_SEARCH_INDEX = ApiBase + "/configuration/searchIndex";
    public static final String CONFIGURATION_VALIDATE_AUDIT_ASSISTANT_CONNECTION = ApiBase + "/configuration/validateAuditAssistantConnection";
    public static final String CONFIGURATION_VALIDATE_REPORT_CONNECTION = ApiBase + "/configuration/validateReportConnection";
    public static String CONFIGURATION(String configId) {
        return String.format(ApiBase + "/configuration/%s", configId);
    }
    public static final String CORE_RULEPACKS = ApiBase + "/coreRulepacks";
    public static String CORE_RULEPACK(String rulepackId) {
        return String.format(ApiBase + "/coreRulepacks/%s", rulepackId);
    }
    public static final String CUSTOM_TAGS = ApiBase + "/customTags";
    public static String CUSTOM_TAG(String tagId) {
        return String.format(ApiBase + "/customTags/%s", tagId);
    }
    public static final String DASHBOARD_VERSIONS = ApiBase + "/dashboardVersions";
    public static final String DATA_EXPORTS = ApiBase + "/dataExports";
    public static final String DATA_EXPORTS_ACTION_EXPORT_AUDIT_TO_CSV = ApiBase + "/dataExports/action/exportAuditToCsv";
    public static final String DATA_EXPORTS_ACTION_EXPORT_ISSUE_STATS_TO_CSV = ApiBase + "/dataExports/action/exportIssueStatsToCsv";
    public static final String DATA_EXPORTS_ACTION_EXPORT_OSC_TO_CSV = ApiBase + "/dataExports/action/exportOscToCsv";
    public static String DATA_EXPORT(String dataExportId) {
        return String.format(ApiBase + "/dataExports/%s", dataExportId);
    }
    public static final String ENGINE_TYPES = ApiBase + "/engineTypes";
    public static final String EVENTS = ApiBase + "/events";
    public static final String FEATURES = ApiBase + "/features";
    public static String FEATURE(String featureId) {
        return String.format(ApiBase + "/features/%s", featureId);
    }
    public static final String FILE_TOKENS = ApiBase + "/fileTokens";
    public static final String FOLDERS = ApiBase + "/folders";
    public static String IID_MIGRATION(String iidMigrationId) {
        return String.format(ApiBase + "/iidMigrations/%s", iidMigrationId);
    }
    public static final String INTERNAL_CUSTOM_TAGS = ApiBase + "/internalCustomTags";
    public static final String ISSUE_DETAILS = ApiBase + "/issueDetails";
    public static String ISSUE_DETAIL(String issueDetailId) {
        return String.format(ApiBase + "/issueDetails/%s", issueDetailId);
    }
    public static final String ISSUE_TEMPLATES = ApiBase + "/issueTemplates";
    public static String ISSUE_TEMPLATE(String issueTemplateId) {
        return String.format(ApiBase + "/issueTemplates/%s", issueTemplateId);
    }
    public static String ISSUE_TEMPLATE_CUSTOM_TAGS(String issueTemplateId) {
        return String.format(ApiBase + "/issueTemplates/%s/customTags", issueTemplateId);
    }
    public static final String ISSUE_VIEW_TEMPLATES = ApiBase + "/issueViewTemplates";
    public static String ISSUE_VIEW_TEMPLATE(String issueViewTemplateId) {
        return String.format(ApiBase + "/issueViewTemplates/%s", issueViewTemplateId);
    }
    public static final String ISSUE_AGING = ApiBase + "/issueaging";
    public static final String ISSUE_AGING_GROUP = ApiBase + "/issueaginggroup";
    public static final String ISSUES = ApiBase + "/issues";
    public static String ISSUE(String issueId) {
        return String.format(ApiBase + "/issues/%s", issueId);
    }
    public static String ISSUE_ATTACHMENTS(String issueId) {
        return String.format(ApiBase + "/issues/%s/attachments", issueId);
    }
    public static String ISSUE_ATTACHMENT(String issueId, String attachmentId) {
        return String.format(ApiBase + "/issues/%s/attachments/%s", issueId, attachmentId);
    }
    public static String ISSUE_AUDIT_HISTORY(String issueId) {
        return String.format(ApiBase + "/issues/%s/auditHistory", issueId);
    }
    public static String ISSUE_COMMENTS(String issueId) {
        return String.format(ApiBase + "/issues/%s/comments", issueId);
    }
    public static final String JOBS = ApiBase + "/jobs";
    public static final String JOBS_ACTION_CANCEL = ApiBase + "/jobs/action/cancel";
    public static String JOB(String jobId) {
        return String.format(ApiBase + "/jobs/%s", jobId);
    }
    public static String JOB_WARNINGS(String jobId) {
        return String.format(ApiBase + "/jobs/%s/warnings", jobId);
    }
    public static final String LDAP_OBJECTS = ApiBase + "/ldapObjects";
    public static final String LDAP_OBJECTS_ACTION_REFRESH = ApiBase + "/ldapObjects/action/refresh";
    public static final String LDAP_OBJECTS_ACTION_SEARCH_UNREGISTERED = ApiBase + "/ldapObjects/action/searchUnregistered";
    public static String LDAP_OBJECT(String ldapObjectId) {
        return String.format(ApiBase + "/ldapObjects/%s", ldapObjectId);
    }
    public static final String LDAP_SERVERS = ApiBase + "/ldapServers";
    public static final String LDAP_SERVERS_ACTION_TEST = ApiBase + "/ldapServers/action/test";
    public static String LDAP_SERVER(String ldapServerId) {
        return String.format(ApiBase + "/ldapServers/%s", ldapServerId);
    }
    public static final String LICENSE = ApiBase + "/license";
    public static final String LOCAL_GROUPS = ApiBase + "/localGroups";
    public static String LOCAL_GROUP(String localGroupId) {
        return String.format(ApiBase + "/localGroups/%s", localGroupId);
    }
    public static final String LOCAL_USERS = ApiBase + "/localUsers";
    public static final String LOCAL_USERS_ACTION_CHECK_PASSWORD_STRENGTH = ApiBase + "/localUsers/action/checkPasswordStrength";
    public static String LOCAL_USER(String localUserId) {
        return String.format(ApiBase + "/localUsers/%s", localUserId);
    }
    public static final String PERFORMANCE_INDICATORS = ApiBase + "/performanceIndicators";
    public static String PERFORMANCE_INDICATOR(String performanceIndicatorId) {
        return String.format(ApiBase + "/performanceIndicators/%s", performanceIndicatorId);
    }
    public static final String PERMISSIONS = ApiBase + "/permissions";
    public static String PERMISSION(String permissionId) {
        return String.format(ApiBase + "/permissions/%s", permissionId);
    }
    public static String PERMISSION_DEPENDS_ON(String permissionId) {
        return String.format(ApiBase + "/permissions/%s/dependsOn", permissionId);
    }
    public static final String PLUGIN_IMAGE = ApiBase + "/pluginimage";
    public static final String PLUGIN_IMAGE_PARSER = ApiBase + "/pluginimage/parser";
    public static final String PLUGIN_LOCALIZATION = ApiBase + "/pluginlocalization";
    public static final String PLUGINS = ApiBase + "/plugins";
    public static final String PLUGINS_ACTION_DISABLE = ApiBase + "/plugins/action/disable";
    public static final String PLUGINS_ACTION_ENABLE = ApiBase + "/plugins/action/enable";
    public static String PLUGIN(String pluginId) {
        return String.format(ApiBase + "/plugins/%s", pluginId);
    }
    public static final String PORTLETS_ISSUE_AGING = ApiBase + "/portlets/issueaging";
    public static final String PROJECT_VERSIONS = ApiBase + "/projectVersions";
    public static String PROJECT_VERSION_ACTION(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/action", projectVersionId);
    }
    public static final String PROJECT_VERSIONS_ACTION_AUDIT_BY_AUDIT_ASSISTANT = ApiBase + "/projectVersions/action/auditByAuditAssistant";
    public static final String PROJECT_VERSIONS_ACTION_COPY_CURRENT_STATE = ApiBase + "/projectVersions/action/copyCurrentState";
    public static final String PROJECT_VERSIONS_ACTION_COPY_FROM_PARTIAL = ApiBase + "/projectVersions/action/copyFromPartial";
    public static final String PROJECT_VERSIONS_ACTION_PURGE = ApiBase + "/projectVersions/action/purge";
    public static final String PROJECT_VERSIONS_ACTION_REFRESH = ApiBase + "/projectVersions/action/refresh";
    public static final String PROJECT_VERSIONS_ACTION_TEST = ApiBase + "/projectVersions/action/test";
    public static final String PROJECT_VERSIONS_ACTION_TRAIN_AUDIT_ASSISTANT = ApiBase + "/projectVersions/action/trainAuditAssistant";
    public static String PROJECT_VERSION(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s", projectVersionId);
    }
    public static String PROJECT_VERSION_ARTIFACTS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/artifacts", projectVersionId);
    }
    public static String PROJECT_VERSION_ATTRIBUTES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/attributes", projectVersionId);
    }
    public static String PROJECT_VERSION_ATTRIBUTE(String projectVersionId, String attributeId) {
        return String.format(ApiBase + "/projectVersions/%s/attributes/%s", projectVersionId, attributeId);
    }
    public static String PROJECT_VERSION_AUDIT_ASSISTANT_STATUS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/auditAssistantStatus", projectVersionId);
    }
    public static String PROJECT_VERSION_AUDIT_ASSISTANT_TRAINING_STATUS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/auditAssistantTrainingStatus", projectVersionId);
    }
    public static String PROJECT_VERSION_AUTH_ENTITIES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/authEntities", projectVersionId);
    }
    public static String PROJECT_VERSION_AUTH_ENTITY(String projectVersionId, String authEntityId) {
        return String.format(ApiBase + "/projectVersions/%s/authEntities/%s", projectVersionId, authEntityId);
    }
    public static String PROJECT_VERSION_BUG_FILING_REQUIREMENTS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugfilingrequirements", projectVersionId);
    }
    public static String PROJECT_VERSION_BUG_FILING_REQUIREMENTS_ACTION_LOGIN(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugfilingrequirements/action/login", projectVersionId);
    }
    public static String PROJECT_VERSION_BUG_FILING_REQUIREMENTS_ACTION_REFRESH(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugfilingrequirements/action/refresh", projectVersionId);
    }
    public static String PROJECT_VERSION_BUG_TRACKER(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugtracker", projectVersionId);
    }
    public static String PROJECT_VERSION_BUG_TRACKER_ACTION_CLEAR_BUG_LINKS_BY_EXTERNAL_IDS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugtracker/action/clearBugLinksByExternalIds", projectVersionId);
    }
    public static String PROJECT_VERSION_BUG_TRACKER_ACTION_TEST(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/bugtracker/action/test", projectVersionId);
    }
    public static String PROJECT_VERSION_CUSTOM_TAGS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/customTags", projectVersionId);
    }
    public static String PROJECT_VERSION_CUSTOM_TAG(String projectVersionId, String customTagId) {
        return String.format(ApiBase + "/projectVersions/%s/customTags/%s", projectVersionId, customTagId);
    }
    public static String PROJECT_VERSION_DYNAMIC_SCAN_REQUEST_TEMPLATE(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/dynamicScanRequestTemplate", projectVersionId);
    }
    public static String PROJECT_VERSION_DYNAMIC_SCAN_REQUESTS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/dynamicScanRequests", projectVersionId);
    }
    public static String PROJECT_VERSION_DYNAMIC_SCAN_REQUESTS_ACTION_CANCEL(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/dynamicScanRequests/action/cancel", projectVersionId);
    }
    public static String PROJECT_VERSION_DYNAMIC_SCAN_REQUEST(String projectVersionId, String dynamicScanRequestId) {
        return String.format(ApiBase + "/projectVersions/%s/dynamicScanRequests/%s", projectVersionId, dynamicScanRequestId);
    }
    public static String PROJECT_VERSION_DYNAMIC_SCAN_REQUESTS_SUMMARY(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/dynamicScanRequestsSummary", projectVersionId);
    }
    public static String PROJECT_VERSION_FILTER_SETS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/filterSets", projectVersionId);
    }
    public static String PROJECT_VERSION_FOLDERS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/folders", projectVersionId);
    }
    public static String PROJECT_VERSION_FOLDER(String projectVersionId, String folderId) {
        return String.format(ApiBase + "/projectVersions/%s/folders/%s", projectVersionId, folderId);
    }
    public static String PROJECT_VERSION_IID_MIGRATIONS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/iidMigrations", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE_ASSIGNMENTS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issueAssignment", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE_ASSIGNMENT(String projectVersionId, String issueAssignmentId) {
        return String.format(ApiBase + "/projectVersions/%s/issueAssignment/%s", projectVersionId, issueAssignmentId);
    }
    public static String PROJECT_VERSION_ISSUE_GROUPS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issueGroups", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE_SELECTOR_SET(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issueSelectorSet", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE_STATISTICS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issueStatistics", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE_SUMMARIES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issueSummaries", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_ACTION_ASSIGN_USER(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/action/assignUser", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_ACTION_AUDIT(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/action/audit", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_ACTION_FILE_BUG(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/action/fileBug", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_ACTION_SUPPRESS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/action/suppress", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_ACTION_UPDATE_TAG(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/action/updateTag", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUES_OPEN_SOURCE(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/openSource", projectVersionId);
    }
    public static String PROJECT_VERSION_ISSUE(String projectVersionId, String issueId) {
        return String.format(ApiBase + "/projectVersions/%s/issues/%s", projectVersionId, issueId);
    }
    public static String PROJECT_VERSION_PERFORMANCE_INDICATOR_HISTORIES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/performanceIndicatorHistories", projectVersionId);
    }
    public static String PROJECT_VERSION_PERFORMANCE_INDICATOR_HISTORIE(String projectVersionId, String performanceIndicatorHistoryId) {
        return String.format(ApiBase + "/projectVersions/%s/performanceIndicatorHistories/%s", projectVersionId, performanceIndicatorHistoryId);
    }
    public static String PROJECT_VERSION_RESPONSIBILITIES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/responsibilities", projectVersionId);
    }
    public static String PROJECT_VERSION_RESPONSIBILITY(String projectVersionId, String responsibilityId) {
        return String.format(ApiBase + "/projectVersions/%s/responsibilities/%s", projectVersionId, responsibilityId);
    }
    public static String PROJECT_VERSION_RESULT_PROCESSING_RULES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/resultProcessingRules", projectVersionId);
    }
    public static String PROJECT_VERSION_SOURCE_FILES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/sourceFiles", projectVersionId);
    }
    public static String PROJECT_VERSION_USER_ISSUE_SEARCH_OPTIONS(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/userIssueSearchOptions", projectVersionId);
    }
    public static String PROJECT_VERSION_VARIABLE_HISTORIES(String projectVersionId) {
        return String.format(ApiBase + "/projectVersions/%s/variableHistories", projectVersionId);
    }
    public static String PROJECT_VERSION_VARIABLE_HISTORIE(String projectVersionId, String varHistoryId) {
        return String.format(ApiBase + "/projectVersions/%s/variableHistories/%s", projectVersionId, varHistoryId);
    }
    public static final String PROJECTS = ApiBase + "/projects";
    public static final String PROJECTS_ACTION_TEST = ApiBase + "/projects/action/test";
    public static String PROJECT(String projectId) {
        return String.format(ApiBase + "/projects/%s", projectId);
    }
    public static String PROJECT_VERSIONS_LIST(String projectprojectId) {
        return String.format(ApiBase + "/projects/%s/versions", projectprojectId);
    }
    public static final String REPORT_DEFINITIONS = ApiBase + "/reportDefinitions";
    public static String REPORT_DEFINITION(String reportDefinitionId) {
        return String.format(ApiBase + "/reportDefinitions/%s", reportDefinitionId);
    }
    public static final String REPORT_LIBRARIES = ApiBase + "/reportLibraries";
    public static String REPORT_LIBRARIE(String reportLibraryId) {
        return String.format(ApiBase + "/reportLibraries/%s", reportLibraryId);
    }
    public static final String REPORTS = ApiBase + "/reports";
    public static String REPORT(String reportId) {
        return String.format(ApiBase + "/reports/%s", reportId);
    }
    public static final String ROLES = ApiBase + "/roles";
    public static String ROLE(String roleId) {
        return String.format(ApiBase + "/roles/%s", roleId);
    }
    public static String ROLE_PERMISSIONS(String roleId) {
        return String.format(ApiBase + "/roles/%s/permissions", roleId);
    }
    public static String SCAN(String scanId) {
        return String.format(ApiBase + "/scans/%s", scanId);
    }
    public static final String SEED_BUNDLES = ApiBase + "/seedBundles";
    public static final String SYSTEM_CONFIGURATIONS = ApiBase + "/systemConfiguration";
    public static String SYSTEM_CONFIGURATION(String name) {
        return String.format(ApiBase + "/systemConfiguration/%s", name);
    }
    public static final String TOKEN_DEFINITIONS = ApiBase + "/tokenDefinitions";
    public static final String TOKENS = ApiBase + "/tokens";
    public static final String TOKENS_ACTION_EXCHANGE_AUTH_CODE = ApiBase + "/tokens/action/exchangeAuthCode";
    public static final String TOKENS_ACTION_GENERATE_AUTH_CODE = ApiBase + "/tokens/action/generateAuthCode";
    public static final String TOKENS_ACTION_REVOKE = ApiBase + "/tokens/action/revoke";
    public static String TOKEN(String tokenID) {
        return String.format(ApiBase + "/tokens/%s", tokenID);
    }
    public static final String UPDATE_RULEPACKS = ApiBase + "/updateRulepacks";
    public static final String USER_ISSUE_SEARCH_OPTIONS = ApiBase + "/userIssueSearchOptions";
    public static final String USER_SESSION_INFO = ApiBase + "/userSession/info";
    public static final String USER_SESSION_PREFS = ApiBase + "/userSession/prefs";
    public static final String USER_SESSION_STATE = ApiBase + "/userSession/state";
    public static final String VALIDATE_EQUATION = ApiBase + "/validateEquation";
    public static final String VALIDATE_SEARCH_STRING = ApiBase + "/validateSearchString";
    public static final String VARIABLES = ApiBase + "/variables";
    public static String VARIABLE(String variableId) {
        return String.format(ApiBase + "/variables/%s", variableId);
    }
    public static final String WEBHOOKS = ApiBase + "/webhooks";
    public static String WEBHOOK(String webHookId) {
        return String.format(ApiBase + "/webhooks/%s", webHookId);
    }
    public static String WEBHOOK_HISTORY_LIST(String webHookId) {
        return String.format(ApiBase + "/webhooks/%s/history", webHookId);
    }
    public static String WEBHOOK_HISTORY_ACTION_RESEND(String webHookId) {
        return String.format(ApiBase + "/webhooks/%s/history/action/resend", webHookId);
    }
    public static String WEBHOOK_HISTORY(String webHookId, String historyId) {
        return String.format(ApiBase + "/webhooks/%s/history/%s", webHookId, historyId);
    }

    // FOR DOWNLOAD & UPLOAD
    public static String DOWNLOAD_ARTIFACT(String artifactId, boolean includeSource) {
        return String.format("/download/artifactDownload.html?mat={downloadToken}&id=%s&includeSource=%b", artifactId, includeSource);
    }

    public static String DOWNLOAD_REPORT_LIBRARY(String reportLibraryId) {
        return String.format("/download/reportLibraryDownload.html?mat={downloadToken}&id=%s", reportLibraryId);
    }

    public static String DOWNLOAD_REPORT_DEFINITION_TEMPLATE(String reportTemplateId) {
        return String.format("/download/reportDefinitionTemplateDownload.html?mat={downloadToken}&id=%s", reportTemplateId);
    }

    public static String DOWNLOAD_RULE_PACK(String rulePackId) {
        return String.format("/download/rulepackDownload.html?mat={downloadToken}&id=%s", rulePackId);
    }

    public static String DOWNLOAD_PROJECT_TEMPLATE(String projectTemplateId) {
        return String.format("/download/projectTemplateDownload.html?mat={downloadToken}&guid=%s", projectTemplateId);
    }

    public static String DOWNLOAD_CURRENT_FPR(String applicationVersionId, boolean includeSource) {
        return String.format("/download/currentStateFprDownload.html?mat={downloadToken}&id=%s&includeSource=%b&clientVersion=16.10", applicationVersionId, includeSource);
    }

    public static String DOWNLOAD_CLOUDSCAN_FPR(String jobToken) {
        return String.format("/download/cloudScanFprDownload.html?mat={downloadToken}&jobToken=%s", jobToken);
    }

    public static String DOWNLOAD_CLOUDSCAN_LOG(String jobToken) {
        return String.format("/download/cloudScanLogDownload.html?mat={downloadToken}&jobToken=%s", jobToken);
    }

    public static String UPLOAD_RESULT_FILE(String applicationVersionId){
        return String.format("/upload/resultFileUpload.html?mat={uploadToken}&entityId=%s", applicationVersionId);
    }

    public static String UPLOAD_REPORT_LIBRARY = "/upload/reportLibraryUpload.html?mat={uploadToken}&UPDATE_LIBRARY=false";

    public static String UPLOAD_UPDATED_REPORT_LIBRARY(String libraryId){
        return String.format("/upload/reportLibraryUpload.html?mat={uploadToken}&UPDATE_LIBRARY_ID=%s&UPDATE_LIBRARY=true",
                libraryId
        );
    }

    public static String UPLOAD_REPORT_DEFINITION_TEMPLATE = "/upload/reportDefinitionTemplateUpload.html?mat={uploadToken}&UPDATE_DEFINITION=false";

    public static String UPLOAD_UPDATED_REPORT_DEFINITION_TEMPLATE(String reportDefinitionTemplateId){
        return String.format(
                "/upload/reportDefinitionTemplateUpload.html?mat={uploadToken}&UPDATE_DEFINITION_ID=%s&UPDATE_DEFINITION=true",
                reportDefinitionTemplateId
        );
    }

    public static String UPLOAD_RULE_PACK = "/upload/rulepackUpload.html?mat={uploadToken}";

    public static String UPLOAD_PLUGIN = "/upload/pluginFileUpload.html?mat={uploadToken}";

    public static String UPLOAD_PROJECT_TEMPLATE(String projectTemplateName){
        return String.format("/upload/projectTemplateUpload.html?mat={uploadToken}&name=%s", projectTemplateName);
    }
}
