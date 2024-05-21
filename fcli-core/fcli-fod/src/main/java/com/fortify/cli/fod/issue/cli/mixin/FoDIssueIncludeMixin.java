package com.fortify.cli.fod.issue.cli.mixin;

import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine;
import java.util.List;

public class FoDIssueIncludeMixin implements IHttpRequestUpdater {
    @CommandLine.Option(names = {"--include", "-i"}, split = ",", descriptionKey = "fcli.fod.issue.list.includeIssue") // use similar attributes as other multi-value options in fcli
    private List<FoDIssueInclude> includes;

    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        for ( var include : includes) {
            request = request.queryString(include.getRequestParameterName(), "true");
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