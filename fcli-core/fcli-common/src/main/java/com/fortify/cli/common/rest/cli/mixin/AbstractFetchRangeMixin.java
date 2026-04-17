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
package com.fortify.cli.common.rest.cli.mixin;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.rest.paging.FetchRange;
import com.fortify.cli.common.rest.paging.FetchRangeConverter;
import com.fortify.cli.common.rest.paging.IPagingSuppressor;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;

import kong.unirest.HttpRequest;
import picocli.CommandLine.Option;

public abstract class AbstractFetchRangeMixin implements IHttpRequestUpdater, IPagingSuppressor {
    @Option(names = "--fetch", paramLabel = "<fetch-range>",
            description = "Limit the number of records fetched from the server. Format: [<start>-]<end>, where start and end are 1-based, inclusive record numbers. Examples: 10 (first 10 records), 1-10 (same), 21-30 (records 21 through 30). By default, all records are fetched using API paging.",
            converter = FetchRangeConverter.class)
    private FetchRange fetchRange;

    public final boolean isFetchSpecified() {
        return fetchRange != null;
    }

    @Override
    public final boolean isPagingSuppressed() {
        return fetchRange != null;
    }

    @Override
    public final HttpRequest<?> updateRequest(HttpRequest<?> request) {
        assertNoExistingPagingParams(request);
        if ( fetchRange == null ) { return request; }
        return applyFetchParams(request, fetchRange);
    }

    protected abstract HttpRequest<?> applyFetchParams(HttpRequest<?> request, FetchRange range);

    private void assertNoExistingPagingParams(HttpRequest<?> request) {
        var url = request.getUrl();
        if ( url.matches(".*[?&](limit|start|offset)=.*") ) {
            throw new FcliBugException("Request URL contains hardcoded paging params: " + url);
        }
    }
}
