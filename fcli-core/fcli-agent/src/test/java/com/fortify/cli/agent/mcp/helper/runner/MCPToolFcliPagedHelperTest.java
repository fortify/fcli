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
package com.fortify.cli.agent.mcp.helper.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;

class MCPToolFcliPagedHelperTest {
    @Test
    void scopeJobIdPrefixesCurrentAuthScopeKey() {
        FcliExecutionContextHolder.pushNew();
        try {
            FcliExecutionContextHolder.current().setMcpRequestAuthScopeKey("ssc|hashed-auth");

            var scopedJobId = MCPToolFcliPagedHelper.scopeJobId("fcli_fn_sscAppListStream:{\"name\":\"demo\"}");

            assertEquals("ssc|hashed-auth|fcli_fn_sscAppListStream:{\"name\":\"demo\"}", scopedJobId);
        } finally {
            FcliExecutionContextHolder.pop();
        }
    }

    @Test
    void scopeJobIdDiffersAcrossAuthScopesForSameSemanticJobId() {
        var semanticJobId = "fcli_fn_sscAppListStream:{\"name\":\"demo\"}";
        String sscScopedJobId;
        String fodScopedJobId;

        FcliExecutionContextHolder.pushNew();
        try {
            FcliExecutionContextHolder.current().setMcpRequestAuthScopeKey("ssc|hashed-auth-1");
            sscScopedJobId = MCPToolFcliPagedHelper.scopeJobId(semanticJobId);
        } finally {
            FcliExecutionContextHolder.pop();
        }

        FcliExecutionContextHolder.pushNew();
        try {
            FcliExecutionContextHolder.current().setMcpRequestAuthScopeKey("ssc|hashed-auth-2");
            fodScopedJobId = MCPToolFcliPagedHelper.scopeJobId(semanticJobId);
        } finally {
            FcliExecutionContextHolder.pop();
        }

        assertNotEquals(sscScopedJobId, fodScopedJobId);
        assertEquals("ssc|hashed-auth-1|" + semanticJobId, sscScopedJobId);
        assertEquals("ssc|hashed-auth-2|" + semanticJobId, fodScopedJobId);
    }
}