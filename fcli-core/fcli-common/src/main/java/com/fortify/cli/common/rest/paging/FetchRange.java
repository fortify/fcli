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
package com.fortify.cli.common.rest.paging;

/**
 * Represents a fetch range consisting of an offset (start record, 0-based) and
 * a maximum number of records to retrieve (limit). Used by fetch-range mixins to
 * restrict paging to a specific slice of server-side results.
 */
public record FetchRange(int offset, int limit) {}
