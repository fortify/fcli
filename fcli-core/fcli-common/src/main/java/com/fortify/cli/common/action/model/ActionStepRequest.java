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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;
import com.fortify.cli.common.util.StringUtils;

import kong.unirest.HttpMethod;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This class describes a REST request.
 */
@Reflectable @NoArgsConstructor
@Data @EqualsAndHashCode(callSuper = true)
public final class ActionStepRequest extends AbstractActionStep {
    @JsonPropertyDescription("Required string: Name to assign to the JSON response for this REST request. Can be referenced in subsequent steps using ${[name]} to access transformed data (if applicable) or ${[name]_raw} to access raw, untransformed data.")
    @JsonProperty(required = true) private String name;
    
    @JsonPropertyDescription("Optional string: HTTP method like GET or POST to use for this REST request. Defaults to 'GET'.")
    @JsonProperty(required = false, defaultValue = "GET") private String method = HttpMethod.GET.name();
    
    @JsonPropertyDescription("Required SpEL template expression: Unqualified REST URI, like '/api/v3/some/api/${parameters.[name].id}' to be appended to the base URL as configured for the given 'target'.")
    @JsonProperty(required = true) private TemplateExpression uri;
    
    @JsonPropertyDescription("Required string if no default target has been configured through defaults.requestTarget: Target on which to execute the REST request. This may be 'fod' (for actions in FoD module), 'ssc' (for actions in SSC module), or a custom request target as configured through 'addRequestTargets'.")
    @JsonProperty(required = false) private String target;
    
    @JsonPropertyDescription("Optional map(string,SpEL template expression): Map of query parameters and corresponding values, for example 'someParam: ${[name].[property]}'.")
    @JsonProperty(required = false) private Map<String,TemplateExpression> query;
    
    @JsonPropertyDescription("Optional SpEL template expression: Request body to send with the REST request.")
    @JsonProperty(required = false) private TemplateExpression body;
    
    @JsonPropertyDescription("Optional enum value: Flag to indicate whether this is a 'paged' or 'simple' request. If set to 'paged' (only available for 'fod' and 'ssc' request targets for now), all pages will be automatically processed. Defaults to 'simple'.")
    @JsonProperty(required = false, defaultValue = "simple") private ActionStepRequest.ActionStepRequestType type = ActionStepRequestType.simple;

    @JsonPropertyDescription("Optional object: Progress messages for various stages of request/response processing.")
    @JsonProperty(required = false) private ActionStepRequest.ActionStepRequestPagingProgressDescriptor pagingProgress;
    
    @JsonPropertyDescription("Optional list: Steps to be executed on the overall response before executing any 'forEach' steps.")
    @JsonProperty(required = false) private List<ActionStep> onResponse;
    
    @JsonPropertyDescription("Optional list: Steps to be executed on request failure. If not specified, an exception will be thrown on request failure.")
    @JsonProperty(required = false) private List<ActionStep> onFail;

    @JsonPropertyDescription("Optional object: Steps to be executed for each record in the REST response.")
    @JsonProperty(required = false) private ActionStepRequest.ActionStepRequestForEachDescriptor forEach;
    
    /**
     * This method is invoked by {@link ActionStep#postLoad()}
     * method. It checks that required properties are set.
     */
    public final void postLoad(Action action) {
        Action.checkNotBlank("request name", name, this);
        Action.checkNotNull("request uri", uri, this);
        if ( StringUtils.isBlank(target) && action.getDefaults()!=null ) {
            target = action.getDefaults().getRequestTarget();
        }
        Action.checkNotBlank("request target", target, this);
        if ( pagingProgress!=null ) {
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
    public static final class ActionStepRequestForEachDescriptor extends AbstractActionStepForEach implements IActionStepIfSupplier {
        private List<ActionStepRequest> embed;
        
        protected final void _postLoad(Action action) {
            //throw new RuntimeException("test");
        }
    }
    
    @Reflectable
    public static enum ActionStepRequestType {
        simple, paged
    }
    
    @Reflectable @NoArgsConstructor
    @Data
    public static final class ActionStepRequestPagingProgressDescriptor {
        private TemplateExpression prePageLoad;
        private TemplateExpression postPageLoad;
        private TemplateExpression postPageProcess;
    }
}