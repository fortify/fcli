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
package com.fortify.cli.aviator.dast;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * Represents a DAST session from WebInspect scan results.
 * A session contains the HTTP request/response context and zero or more issues.
 */
@Data
public class DastSession {
    private String requestId;
    private String url;
    private String scheme;
    private String host;
    private int port;
    private String attackParamDescriptor;

    // Decoded raw HTTP request/response (Base64 decoded)
    private String rawRequest;
    private String rawResponse;

    // Issues found in this session
    private List<DastIssue> issues = new ArrayList<>();

    /**
     * Check if this session has any issues.
     */
    public boolean hasIssues() {
        return issues != null && !issues.isEmpty();
    }

    /**
     * Get the number of issues in this session.
     */
    public int getIssueCount() {
        return issues != null ? issues.size() : 0;
    }
}
