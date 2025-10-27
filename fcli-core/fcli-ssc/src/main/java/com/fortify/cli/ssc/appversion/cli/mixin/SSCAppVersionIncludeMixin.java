/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.ssc.appversion.cli.mixin;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class SSCAppVersionIncludeMixin implements IHttpRequestUpdater, IRecordTransformer {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--include", "-i"}, split = ",", defaultValue = "active", descriptionKey = "fcli.ssc.appversion.list.include", paramLabel="<values>")
    private Set<SSCAppVersionInclude> includes;

    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        if ( includes!=null ) {
            for ( var include : includes) {
                var queryParameterName = include.getRequestParameterName();
                if ( queryParameterName!=null ) {
                    request = request.queryString(queryParameterName, "true");
                }
            }
        }
        return request;
    }
    
    @Override
    public JsonNode transformRecord(JsonNode record) {
        // If includes doesn't include 'active', we return null for any active application versions
        // to remove those from the results. It would be more performant to add q=active:false request
        // parameter instead to have SSC filter out active versions, but we need to figure out how
        // to properly combine this with 'q'-parameters generated through SSCQParamGenerator as used
        // in the 'fcli ssc av ls' command.
        return !includes.contains(SSCAppVersionInclude.active)
                && JsonHelper.evaluateSpelExpression(record, "active", Boolean.class)
                ? null
                : record;
    }

    @RequiredArgsConstructor
    public static enum SSCAppVersionInclude {
        active(null), inactive("includeInactive");

        @Getter
        private final String requestParameterName;
    }
}
