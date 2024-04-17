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
package com.fortify.cli.common.action.helper.descriptor;

import java.util.Map;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spring.expression.wrapper.TemplateExpression;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This class describes a request target.
 */
@Reflectable @NoArgsConstructor
@Data
public final class ActionRequestTargetDescriptor {
    /** Required name */
    private String name;
    /** Required base URL */
    private TemplateExpression baseUrl;
    /** Optional headers */
    private Map<String, TemplateExpression> headers;
    // TODO Add support for next page URL producer
    // TODO ? Add proxy support ?
    
    public final void postLoad(ActionDescriptor action) {
        ActionDescriptor.checkNotBlank("request target name", name, this);
        ActionDescriptor.checkNotNull("request target base URL", baseUrl, this);
    }
}