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
package com.fortify.cli.fod._common.scan.helper.compare;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Scan ids and scan summaries (as returned by /api/v3/scans/{scanId}/summary) for the target scan
 * and the previous completed scan of the same scan type and assessment type.
 */
public record FoDScanComparePair(String targetScanId, JsonNode target, String previousScanId, JsonNode previous) {}
