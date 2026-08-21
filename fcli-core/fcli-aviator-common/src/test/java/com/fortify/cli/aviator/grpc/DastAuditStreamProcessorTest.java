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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.dastaudit.DastAuditClientMessage;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.DastSession;

import io.grpc.Status;

class DastAuditStreamProcessorTest {
    @Test
    void registersAllRequestsBeforeReturningMessagesForSending() {
        var processor = new DastAuditStreamProcessor(null, null, null, 0);

        List<DastAuditClientMessage> requests = processor.prepareAuditRequests(
            List.of(workItem("DAST-1"), workItem("DAST-2")), "stream-1");

        assertEquals(List.of("DAST-1", "DAST-2"), requests.stream()
            .map(request -> request.getAudit().getFinding().getIssueId())
            .toList());
        assertEquals(2, processor.pendingRequestCount());
    }

    @Test
    void reusesRequestIdsAndRequeuesOnlyUnfinishedWork() {
        var processor = new DastAuditStreamProcessor(null, null, null, 0);
        List<DastAuditWorkItem> workItems = List.of(workItem("DAST-1"), workItem("DAST-2"));
        processor.initializeWorkItems(workItems);
        List<DastAuditClientMessage> firstAttempt = processor.prepareAuditRequests(workItems, "stream-1");

        processor.completeRequest(firstAttempt.get(0).getAudit().getRequestId());
        List<DastAuditClientMessage> retry = processor.prepareAuditRequests(
            processor.remainingWorkItems(), "stream-2");

        assertEquals(1, retry.size());
        assertEquals("DAST-2", retry.get(0).getAudit().getFinding().getIssueId());
        assertEquals(firstAttempt.get(1).getAudit().getRequestId(), retry.get(0).getAudit().getRequestId());
        assertEquals("stream-2", retry.get(0).getAudit().getStreamId());
    }

    @Test
    void retriesOnlyTransportDisconnections() {
        assertTrue(DastAuditStreamProcessor.isRetryableError(Status.UNAVAILABLE.asRuntimeException()));
        assertTrue(DastAuditStreamProcessor.isRetryableError(
            Status.INTERNAL.withDescription("RST_STREAM closed").asRuntimeException()));
        assertTrue(DastAuditStreamProcessor.isInfiniteRetryError(
            Status.INTERNAL.withDescription("PROTOCOL_ERROR").asRuntimeException()));
        assertFalse(DastAuditStreamProcessor.isRetryableError(Status.INVALID_ARGUMENT.asRuntimeException()));
    }

    private DastAuditWorkItem workItem(String issueId) {
        var issue = new DastIssue();
        issue.setId(issueId);
        return new DastAuditWorkItem(new DastSession(), issue);
    }
}