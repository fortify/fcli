/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.schema.SampleYamlSnippets;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import kong.unirest.HttpMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a REST request.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonTypeName("rest.call")
@JsonClassDescription("Define a REST call, like request method, URI, ...")
@SampleYamlSnippets("""
        steps:
          - rest.call:
              pvs:                            # Name for this REST call for later reference
                target: ssc                   # Default configured through config::rest.target.default
                method: GET                   # Default: GET
                uri: /api/v1/projectVersions  # URI
                query:                        # Query string parameters
                  fields: id,name,project     # May also use expressions
                type: paged                   # simple or paged
                records.for-each:             # Iterate through response records
                  record.var-name: pv         # Variable name to hold current record
                  embed:                      # For each record, embed data from other REST call
                    artifacts:                # Accessible through ${pv.artifacts}
                      uri: /api/v1/projectVersions/${pv.id}/artifacts
                  do:
                    - ...                     # Steps to execute for each response record
        """)
public final class ActionStepRestCallEntry extends AbstractActionElementIf implements IMapKeyAware<String> {
    @JsonIgnore private String key;
    
    @JsonPropertyDescription("""
        Optional string: HTTP method like GET or POST to use for this REST request. Defaults value: GET.    
        """)
    @JsonProperty(value = "method", required = false, defaultValue = "GET") private String method = HttpMethod.GET.name();
    
    @JsonPropertyDescription("""
        Required SpEL template expression: Unqualified REST URI, like '/api/v3/some/api/${var.id}' to be \
        appended to the base URL provided by the given 'target'.
        """)
    @JsonProperty(value = "uri", required = true) private TemplateExpression uri;
    
    @JsonPropertyDescription("""
        Required string if no default target has been configured through config:rest.target.default. \
        Target on which to execute the REST request. Third-party request targets can be configured \
        through 'rest.target' steps; such steps should appear before the 'rest.call' steps that \
        reference these request targets. In additionan, fcli provides the 'fod' target for actions \
        run through 'fcli fod action run', and the 'ssc', 'sc-sast', and 'sc-dast' targets for \
        actions run through the 'fcli ssc action run' command. These fcli-provided targets integrate \
        with fcli session management.     
        """)
    @JsonProperty(value = "target", required = false) private String target;
    
    @JsonPropertyDescription("""
        Optional map: Query parameters to be added to the request. Map keys specify the query parameter name, \
        map values specify the query parameter value. Keys must be plain strings, values are evaluated as \
        SpEL template expressions, for example 'someParam: ${var1.prop1]}'.
        """)
    @JsonProperty(value = "query", required = false) private LinkedHashMap<String,TemplateExpression> query;
    
    @JsonPropertyDescription("""
        Optional SpEL template expression: Request body to send with the REST request.
        """)
    @JsonProperty(value = "body", required = false) private TemplateExpression body;
    
    @JsonPropertyDescription("""
        Optional enum value: Flag to indicate whether this is a 'paged' or 'simple' request. If set to 'paged' \
        (for now only supported for built-in request targets like 'fod' or 'ssc'), the request will be repeated \
        with the appropriate paging request parameters to load and process all available pages. Defaults value: simple.
        """)
    @JsonProperty(value = "type", required = false, defaultValue = "simple") private ActionStepRestCallEntry.ActionStepRequestType type = ActionStepRequestType.simple;

    @JsonPropertyDescription("""
        Optional object: Log progress messages during the various stages of request/response processing.
        """)
    @JsonProperty(value = "log.progress", required = false) private ActionStepRestCallEntry.ActionStepRestCallLogProgressDescriptor logProgress;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed on each successfull REST response. For simple requests, these \
        steps will be executed once. For paged requests, these steps will be executed after every individual \
        page has been received. Steps can reference the [requestName] and [requestName]_raw variables to \
        access processed and raw response data respectively. Any steps define in 'on.success' will be executed \
        before processing individual response records through the 'records.for-each' instruction.
        """)
    @JsonProperty(value = "on.success", required = false) private ArrayList<ActionStep> onResponse;
    
    @JsonPropertyDescription("""
        Optional list: Steps to be executed on request failure. If not specified, an exception will be thrown \
        on request failure. Steps can reference a variable named after the identifier for this REST call, for \
        example 'x_exception', to access the Java Exception object that represents the failure that occurred.
        """)
    @JsonProperty(value = "on.fail", required = false) private ArrayList<ActionStep> onFail;

    @JsonPropertyDescription("""
        Optional object: If the processed (successfull) REST response provides an array of records, this \
        instruction allows for executing the steps provided in the 'do' block for each individual record.
        """)
    @JsonProperty(value = "records.for-each", required = false) private ActionStepRestCallEntry.ActionStepRequestForEachResponseRecord forEach;
    
    /**
     * This method is invoked by {@link ActionStep#postLoad()}
     * method. It checks that required properties are set.
     */
    public final void postLoad(Action action) {
        Action.checkNotNull("request uri", uri, this);
        if ( StringUtils.isBlank(target) && action.getConfig()!=null ) {
            target = action.getConfig().getRestTargetDefault();
        }
        Action.checkNotBlank("request target", target, this);
        if ( logProgress!=null ) {
            type = ActionStepRequestType.paged;
        }
    }
    
    /**
     * This class describes a request forEach element, allowing iteration over the output of
     * the parent element, like the response of a REST request or the contents of a
     * action parameter. 
     */
    @Reflectable @NoArgsConstructor
    @Data @EqualsAndHashCode(callSuper = true)
    @JsonTypeName("rest.call-for-each")
    public static final class ActionStepRequestForEachResponseRecord extends AbstractActionElementForEachRecord implements IActionStepIfSupplier {
        @JsonPropertyDescription("""
            Optional map: Allows for making additional REST calls for each individual record being \
            processed. Map values define the REST call to be executed, map keys define under which \
            property the response will be embedded into the variable specified through the 'set.record-var' \
            instruction, with similar behavior as described for the rest.call step, i.e., processed \
            data will be available through a property named after the map key, whereas raw response \
            data will be available through a property named [map key]_raw.
            """)
        @JsonProperty(value = "embed", required = false) private Map<String, ActionStepRestCallEntry> embed;
        
        protected final void _postLoad(Action action) {}
    }
    
    @Reflectable
    public static enum ActionStepRequestType {
        simple, paged
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    @JsonTypeName("rest.call-log.progress")
    public static final class ActionStepRestCallLogProgressDescriptor implements IActionElement {
        @JsonPropertyDescription("""
            Optional SpEL template expression: Log a progress message before loading the next page.
            """)
        @JsonProperty(value = "page.pre-load", required = false) private TemplateExpression prePageLoad;
        
        @JsonPropertyDescription("""
            Optional SpEL template expression: Log a progress message after a page has been loaded.
            """)
        @JsonProperty(value = "page.post-load", required = false) private TemplateExpression postPageLoad;
        
        @JsonPropertyDescription("""
            Optional SpEL template expression: Log a progress message after a page has been processed.
            """)
        @JsonProperty(value = "page.post-process", required = false) private TemplateExpression postPageProcess;
        
        @Override
        public void postLoad(Action action) {
            // TODO Check whether at least one property has a value?
        }
    }
}