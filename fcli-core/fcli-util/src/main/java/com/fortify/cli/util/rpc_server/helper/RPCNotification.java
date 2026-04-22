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
package com.fortify.cli.util.rpc_server.helper;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

/**
 * Builder for JSON-RPC 2.0 notification messages (server-to-client, no {@code id} field).
 * Used by {@link RPCPushJobEventListener} to push job lifecycle events to the RPC client.
 *
 * <p>Notification methods follow the {@code job.*} namespace:
 * <ul>
 *   <li>{@code job.started} — job execution has begun</li>
 *   <li>{@code job.records} — batch of records produced</li>
 *   <li>{@code job.progress} — progress message</li>
 *   <li>{@code job.complete} — job finished (success or error)</li>
 * </ul>
 *
 * @author Ruud Senden
 */
final class RPCNotification {
    private static final ObjectMapper OM = JsonHelper.getObjectMapper();

    private RPCNotification() {}

    /** Build a {@code job.started} notification. */
    static String jobStarted(String jobId, String description) {
        var params = OM.createObjectNode();
        params.put("jobId", jobId);
        params.put("description", description);
        return toJson("job.started", params);
    }

    /** Build a {@code job.records} notification with a batch of records. */
    static String jobRecords(String jobId, List<JsonNode> records) {
        var params = OM.createObjectNode();
        params.put("jobId", jobId);
        ArrayNode arr = params.putArray("records");
        records.forEach(arr::add);
        params.put("count", records.size());
        return toJson("job.records", params);
    }

    /** Build a {@code job.progress} notification. */
    static String jobProgress(String jobId, String message) {
        var params = OM.createObjectNode();
        params.put("jobId", jobId);
        params.put("message", message);
        return toJson("job.progress", params);
    }

    /** Build a {@code job.complete} notification. */
    static String jobComplete(String jobId, int exitCode, String stderr, String stdout) {
        var params = OM.createObjectNode();
        params.put("jobId", jobId);
        params.put("exitCode", exitCode);
        if (stderr != null && !stderr.isBlank()) {
            params.put("stderr", stderr);
        }
        if (stdout != null && !stdout.isBlank()) {
            params.put("stdout", stdout);
        }
        return toJson("job.complete", params);
    }

    private static String toJson(String method, ObjectNode params) {
        var node = OM.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        node.set("params", params);
        try {
            return OM.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize notification", e);
        }
    }
}
