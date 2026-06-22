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
package com.fortify.cli.common.output.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interface for extracting response-level metadata (e.g., total record count,
 * paging links) from a raw API response body before input transformation
 * strips non-record data.
 * <p>
 * Product helpers (e.g., SSCProductHelper, FoDProductHelper) implement this
 * to capture metadata specific to their API response format.
 */
@FunctionalInterface
public interface IResponseMetadataCollector {
    /**
     * Extract metadata from the raw response body. Called before input
     * transformation (which typically extracts only the records array).
     * Returns {@code null} if no metadata is available.
     */
    ObjectNode collectResponseMetadata(JsonNode responseBody);
}
