package com.fortify.cli.ssc.issue.cli.mixin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;

import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Option;

public class SSCIssueIncludeMixin implements IHttpRequestUpdater, IRecordTransformer {
    @DisableTest(TestType.MULTI_OPT_PLURAL_NAME)
    @Option(names = {"--include", "-i"}, split = ",", defaultValue = "visible", descriptionKey = "fcli.ssc.issue.list.includeIssue", paramLabel="<status>")
    private Set<SSCIssueInclude> includes;

    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        // TODO Check whether we can potentially perform any server-side filtering
        //      to retrieve ONLY suppressed/fixed/hidden issues, although SSC doesn't
        //      seem to allow server-side filtering on the respective boolean fields.
        //      See FoDIssueIncludeMixin for additional details.
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
        // If includes doesn't include 'visible', we return null for any visible (non-suppressed
        // & non-fixed) issues. We don't need explicit handling for other cases, as suppressed or
        // fixed issues won't be returned by FoD if not explicitly specified through the --include
        // option.
        return !includes.contains(SSCIssueInclude.visible)
                && JsonHelper.evaluateSpelExpression(record, "!hidden && !removed && !suppressed", Boolean.class)
                ? null
                : addVisibilityProperties((ObjectNode)record);
    }

    private ObjectNode addVisibilityProperties(ObjectNode record) {
        Map<String,String> visibility = new LinkedHashMap<>();
        if ( getBoolean(record, "hidden") )     { visibility.put("hidden", "(H)"); }
        if ( getBoolean(record, "removed") )    { visibility.put("removed", "(R)"); } 
        if ( getBoolean(record, "suppressed") ) { visibility.put("suppressed", "(S)"); }
        if ( visibility.isEmpty() )             { visibility.put("visible", " "); }
        record.put("visibility", String.join(",", visibility.keySet()))
            .put("visibilityMarker", String.join("", visibility.values()));
        return record;
    }

    private boolean getBoolean(ObjectNode record, String propertyName) {
        var field = record.get(propertyName);
        return field==null ? false : field.asBoolean();
    }

    @RequiredArgsConstructor
    public static enum SSCIssueInclude {
        visible(null), hidden("showhidden"), removed("showremoved"), suppressed("showsuppressed");

        @Getter
        private final String requestParameterName;
    }
}
