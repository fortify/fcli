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

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;
import com.fortify.cli.aviator.audit.model.File;
import com.fortify.cli.aviator.audit.model.UserPrompt;

class GrpcUtilTest {

    @Test
    void shouldPreserveResolvedProgrammingLanguages() {
        File file = new File();
        file.setName("db/t1.sql");

        UserPrompt userPrompt = UserPrompt.builder()
                .files(List.of(file))
                .programmingLanguages(Set.of("PLSQL"))
                .fileExtension("sql")
                .language("PLSQL")
                .category("SQL Injection")
                .build();

        com.fortify.aviator.grpc.AuditRequest auditRequest =
                GrpcUtil.convertToAuditRequest(userPrompt, "stream-1", "request-1");

        assertEquals(List.of("PLSQL"), auditRequest.getProgrammingLanguagesList());
        assertEquals("PLSQL", auditRequest.getLanguage());
    }

    @Test
    void shouldClassifySkippedServerResponseWithoutInspectingMessage() {
        AuditorResponse response = AuditorResponse.newBuilder()
                .setStatus("SKIPPED")
                .setStatusMessage("example could not be read from the FPR")
                .build();

        AuditResponse auditResponse = GrpcUtil.convertToAuditResponse(response);

        assertEquals(AuditSkipReason.SKIPPED_BY_AVIATOR, auditResponse.getAuditSkipReason());
    }
}