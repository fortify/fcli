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
@Data
public final class ActionStepRequest implements IActionStep {
    /** Optional if-expression, executing this request only if condition evaluates to true */
    @JsonProperty("if") private TemplateExpression _if;
    /** Required name for this step element */
    private String name;
    /** Optional HTTP method, defaults to 'GET' */
    private String method = HttpMethod.GET.name();
    /** Required template expression defining the request URI from which to get the data */
    private TemplateExpression uri;
    /** Required target to which to send the request; may be specified here or through defaults.requestTarget */
    private String target;
    /** Map defining (non-encoded) request query parameters; parameter values are defined as template expressions */
    private Map<String,TemplateExpression> query;
    /** Optional request body template expression */
    private TemplateExpression body;
    /** Type of request; either 'simple' or 'paged' for now */
    private ActionStepRequest.ActionStepRequestType type = ActionStepRequestType.simple;
    /** Optional progress messages for various stages of request processing */
    private ActionStepRequest.ActionStepRequestPagingProgressDescriptor pagingProgress;
    /** Optional steps to be executed on the response before executing forEach steps */
    private List<ActionStep> onResponse;
    /** Optional steps to be executed on request failure; if not declared, an exception will be thrown */
    private List<ActionStep> onFail;
    /** Optional forEach block to be repeated for every response element */
    private ActionStepRequest.ActionStepRequestForEachDescriptor forEach;
    
    /**
     * This method is invoked by {@link ActionStep#postLoad()}
     * method. It checks that required properties are set.
     */
    public final void postLoad(Action action) {
        Action.checkNotBlank("request name", name, this);
        Action.checkNotNull("request uri", uri, this);
        if ( StringUtils.isBlank(target) && action.defaults!=null ) {
            target = action.defaults.requestTarget;
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