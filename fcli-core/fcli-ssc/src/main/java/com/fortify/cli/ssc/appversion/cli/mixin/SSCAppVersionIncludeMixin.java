package com.fortify.cli.ssc.appversion.cli.mixin;

import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;
import java.util.Set;

public class SSCAppVersionIncludeMixin implements IHttpRequestUpdater {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--include", "-i"}, split = ",", descriptionKey = "fcli.ssc.appversion.list.include", paramLabel="<status>")
    private Set<SSCIssueInclude> includes;

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

    @RequiredArgsConstructor
    public static enum SSCIssueInclude {
        inactive("includeInactive"),
        onlyWithMyIssues("myAssignedIssues"),
        onlyNotEmpty("onlyIfHasIssues");

        @Getter
        private final String requestParameterName;
    }
}
