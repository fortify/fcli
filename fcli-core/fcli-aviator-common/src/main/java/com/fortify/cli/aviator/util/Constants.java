/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.util;

public class Constants {

    // Audit Result Values
    public static final String NOT_AN_ISSUE = "Not an Issue";
    public static final String EXPLOITABLE = "Exploitable";
    public static final String PENDING_REVIEW = "Pending Review/Not Set";
    public static final String FALSE_POSITIVE = "False Positive";
    public static final String SUSPICIOUS = "Suspicious";
    public static final String SANITIZED = "Sanitized";
    public static final String RELIABILITY_ISSUE = "Reliability Issue";
    public static final String BAD_PRACTICE = "Bad Practice";

    // Aviator Specific Audit Values
    public static final String AVIATOR_NOT_AN_ISSUE = "AVIATOR:Not an Issue";
    public static final String AVIATOR_REMEDIATION_REQUIRED = "AVIATOR:Remediation Required";
    public static final String AVIATOR_UNSURE = "AVIATOR:Unsure";
    public static final String AVIATOR_EXCLUDED = "AVIATOR:Excluded due to Limiting";
    public static final String AVIATOR_LIKELY_TP = "AVIATOR:Suspicious";
    public static final String AVIATOR_LIKELY_FP = "AVIATOR:Proposed Not an Issue";
    public static final String PROCESSED_BY_AVIATOR = "PROCESSED_BY_AVIATOR";
    public static final String REMEDIATION_REQUIRED = "Remediation Required";
    public static final String PROPOSED_NOT_AN_ISSUE = "Proposed Not an Issue";
    public static final String UNSURE = "Unsure";

    // Tag IDs
    public static final String AVIATOR_PREDICTION_TAG_ID = "C2D6EC66-CCB3-4FB9-9EE0-0BB02F51008F";
    public static final String AVIATOR_STATUS_TAG_ID = "FB7B0462-2C2E-46D9-811A-DCC1F3C83051";
    public static final String AVIATOR_EXPECTED_OUTCOME_TAG_ID = "013cc66f-8651-4e39-bacb-beb918c5ef65";
    public static final String ANALYSIS_TAG_ID = "87f2364f-dcd4-49e6-861d-f8d3f351686b";
    public static final String AUDITOR_STATUS_TAG_ID = "ACB05E55-E74D-468C-8501-52E1FDC27D71";
    public static final String FOD_TAG_ID = "604f0fbe-b5fe-47cd-a9cb-587ad8ebe93a";

    // User Names
    public static final String USER_NAME = "Fortify Aviator";

    // Other Constants
    public static final String AUDIT_NAMESPACE_URI = "xmlns://www.fortify.com/schema/audit";
    public static final String RESEARCH_TAG_NAME = "LlmAudit";
    public static final String FOD_TAG_NAME = "Auditor Status";

    //Limiting Constants
    public static final int MAX_PER_CATEGORY = 500;
    public static final int MAX_TOTAL = 2500;
    public static final String MAX_PER_CATEGORY_EXCEEDED = "Fortify detected {issues_new_in_category} new issues in this (sub)category. Fortify Aviator auditing was limited to the first {MAX_PER_CATEGORY}.";
    public static final String MAX_TOTAL_EXCEEDED = "Fortify detected {issues_new_total} new issues. Fortify Aviator auditing was limited to {MAX_TOTAL} issues in total, while ensuring that representative issues in each category were audited.";

    // Operation constants for error messages
    public static final String OP_CREATE_APP = "application creation";
    public static final String OP_UPDATE_APP = "application update";
    public static final String OP_DELETE_APP = "application deletion";
    public static final String OP_GET_APP = "retrieving application";
    public static final String OP_LIST_APPS = "listing applications";
    public static final String OP_GENERATE_TOKEN = "token generation";
    public static final String OP_LIST_TOKENS = "listing tokens";
    public static final String OP_LIST_TOKENS_BY_DEVELOPER = "list tokens by developer";
    public static final String OP_REVOKE_TOKEN = "revoking token";
    public static final String OP_DELETE_TOKEN = "deleting token";
    public static final String OP_VALIDATE_TOKEN = "validating token";
    public static final String OP_VALIDATE_USER_TOKEN = "validating user token";
    public static final String OP_LIST_ENTITLEMENTS = "listing entitlements";

    public static final long DEFAULT_PING_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_TIMEOUT_SECONDS = 30;
    public static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    public static final int INITIAL_REQUEST_WINDOW = 10;
    public static final int MAX_RETRIES = 10;
    public static final long BASE_DELAY_MS = 500;
    public static final long MAX_DELAY_MS = 5000;
    public static final int MAX_STREAM_RETRIES = 5;
    public static final long STREAM_RETRY_BASE_DELAY_MS = 2000;
    public static final long STREAM_RETRY_MAX_DELAY_MS = 30000;
}