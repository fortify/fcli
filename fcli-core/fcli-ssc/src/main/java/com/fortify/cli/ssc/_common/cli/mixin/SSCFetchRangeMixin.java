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
package com.fortify.cli.ssc._common.cli.mixin;

import com.fortify.cli.common.rest.cli.mixin.AbstractFetchRangeMixin;
import com.fortify.cli.common.rest.paging.FetchRange;

import kong.unirest.HttpRequest;

public final class SSCFetchRangeMixin extends AbstractFetchRangeMixin {
    @Override
    protected HttpRequest<?> applyFetchParams(HttpRequest<?> request, FetchRange range) {
        return request
                .queryString("start", range.offset())
                .queryString("limit", range.limit());
    }
}
