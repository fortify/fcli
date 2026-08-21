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
package com.fortify.cli.aviator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.DastReproStep;
import com.fortify.cli.aviator.dast.DastSession;

class DastAuditRequestMapperTest {
    @Test
    void mapsCompleteFindingContext() {
        var session = new DastSession();
        session.setRequestId("request-1");
        session.setUrl("https://example.test");
        session.setRawRequest("GET / HTTP/1.1");
        session.setRawResponse("HTTP/1.1 200 OK");
        var issue = new DastIssue();
        issue.setId("DAST-1");
        issue.setName("SQL Injection");
        issue.getClassifications().put("CWE", "Improper Neutralization");
        var step = new DastReproStep();
        step.setSource("Attack");
        step.setUrl("https://example.test?id=1");
        step.setPostParams("id=1");
        issue.getReproSteps().add(step);
        issue.getReproStepUrls().add(step.getUrl());

        var context = DastAuditRequestMapper.toFindingContext(session, issue);

        assertEquals("DAST-1", context.getIssueId());
        assertEquals("GET / HTTP/1.1", context.getRawRequest());
        assertEquals("CWE", context.getClassifications(0).getKind());
        assertEquals("Attack", context.getReproSteps(0).getSource());
        assertEquals("id=1", context.getReproSteps(0).getPostParams());
    }
}