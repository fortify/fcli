package com.fortify.cli.fod.issue.cli.mixin;

import java.util.List;

import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class FoDIssueIncludeMixin implements IHttpRequestUpdater {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--include", "-i"}, split = ",", descriptionKey = "fcli.fod.issue.list.includeIssue", paramLabel="<status>")
    private List<FoDIssueInclude> includes;

    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        if ( includes!=null ) {
            for ( var include : includes) {
                request = request.queryString(include.getRequestParameterName(), "true");
            }
        }
        return request;
    }

    @RequiredArgsConstructor
    public static enum FoDIssueInclude {
        fixed("includeFixed"), suppressed("includeSuppressed");

        @Getter
        private final String requestParameterName;
    }
}
